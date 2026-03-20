package com.yuan.monitor.runtime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.monitor.entity.TestStatusRecord;
import com.yuan.monitor.mapper.TestStatusRecordMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ActiveRunRegistry {
    private static final long DB_FALLBACK_COOLDOWN_MS = 5000L;

    private final TestStatusRecordMapper testStatusRecordMapper;
    private final ConcurrentHashMap<Long, TestStatusRecord> activeRuns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ExpiringRunRecord> recentlyEndedRuns = new ConcurrentHashMap<>();
    private final AtomicLong lastDbReloadAt = new AtomicLong(0L);
    private final long trackingGraceSeconds;

    public ActiveRunRegistry(TestStatusRecordMapper testStatusRecordMapper,
                             @Value("${yuan.monitor.system-metrics.tracking-grace-seconds:0}") long trackingGraceSeconds) {
        this.testStatusRecordMapper = testStatusRecordMapper;
        this.trackingGraceSeconds = trackingGraceSeconds;
    }

    public void onStatusChanged(TestStatusRecord record) {
        if (record == null || record.getRunId() == null) {
            return;
        }

        pruneExpiredEndedRuns();
        if ("RUNNING".equals(record.getStatus())) {
            recentlyEndedRuns.remove(record.getRunId());
            activeRuns.compute(record.getRunId(), (runId, existing) -> isNewer(record, existing) ? copy(record) : existing);
            return;
        }

        activeRuns.computeIfPresent(record.getRunId(), (runId, existing) -> isNewer(record, existing) ? null : existing);
        if (trackingGraceSeconds <= 0) {
            recentlyEndedRuns.remove(record.getRunId());
            return;
        }

        recentlyEndedRuns.compute(record.getRunId(), (runId, existing) -> {
            if (existing != null && !isNewer(record, existing.getRecord())) {
                return existing;
            }
            LocalDateTime effectiveTime = record.getCreatedAt() == null ? LocalDateTime.now() : record.getCreatedAt();
            return new ExpiringRunRecord(copy(record), effectiveTime.plusSeconds(trackingGraceSeconds));
        });
    }

    public List<TestStatusRecord> listActiveRuns() {
        if (activeRuns.isEmpty()) {
            reloadFromDbIfNeeded();
        }
        return sortRecords(new ArrayList<>(activeRuns.values()));
    }

    // 资源采样优先只跟踪真正处于 RUNNING 的 run；只有显式配置了 grace 时，才短暂保留结束 run 补尾部秒。
    public List<TestStatusRecord> listTrackableRuns() {
        if (activeRuns.isEmpty()) {
            reloadFromDbIfNeeded();
        }
        pruneExpiredEndedRuns();

        Map<Long, TestStatusRecord> records = new LinkedHashMap<>();
        activeRuns.forEach((runId, record) -> records.put(runId, copy(record)));
        recentlyEndedRuns.forEach((runId, expiring) -> records.putIfAbsent(runId, copy(expiring.getRecord())));
        return sortRecords(new ArrayList<>(records.values()));
    }

    public TestStatusRecord findLatestRunningTask() {
        return listActiveRuns().stream().findFirst().orElse(null);
    }

    private void reloadFromDbIfNeeded() {
        long now = System.currentTimeMillis();
        long previous = lastDbReloadAt.get();
        if (now - previous < DB_FALLBACK_COOLDOWN_MS) {
            return;
        }
        if (!lastDbReloadAt.compareAndSet(previous, now)) {
            return;
        }

        LambdaQueryWrapper<TestStatusRecord> runningWrapper = new LambdaQueryWrapper<>();
        runningWrapper.eq(TestStatusRecord::getStatus, "RUNNING")
                .orderByDesc(TestStatusRecord::getCreatedAt);

        List<TestStatusRecord> runningCandidates = testStatusRecordMapper.selectList(runningWrapper);
        Map<Long, TestStatusRecord> latestRunning = new LinkedHashMap<>();
        for (TestStatusRecord candidate : runningCandidates) {
            if (candidate.getRunId() == null || latestRunning.containsKey(candidate.getRunId())) {
                continue;
            }

            LambdaQueryWrapper<TestStatusRecord> latestWrapper = new LambdaQueryWrapper<>();
            latestWrapper.eq(TestStatusRecord::getRunId, candidate.getRunId())
                    .orderByDesc(TestStatusRecord::getCreatedAt)
                    .last("limit 1");

            TestStatusRecord latestStatus = testStatusRecordMapper.selectOne(latestWrapper);
            if (latestStatus != null && "RUNNING".equals(latestStatus.getStatus())) {
                latestRunning.put(candidate.getRunId(), copy(latestStatus));
            }
        }

        activeRuns.clear();
        activeRuns.putAll(latestRunning);
    }

    private void pruneExpiredEndedRuns() {
        LocalDateTime now = LocalDateTime.now();
        recentlyEndedRuns.entrySet().removeIf(entry -> {
            LocalDateTime expireAt = entry.getValue().getExpireAt();
            return expireAt != null && expireAt.isBefore(now);
        });
    }

    private List<TestStatusRecord> sortRecords(List<TestStatusRecord> records) {
        return records.stream()
                .map(this::copy)
                .sorted(Comparator.comparing(TestStatusRecord::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private boolean isNewer(TestStatusRecord incoming, TestStatusRecord existing) {
        if (existing == null) {
            return true;
        }
        LocalDateTime incomingTime = incoming.getCreatedAt();
        LocalDateTime existingTime = existing.getCreatedAt();
        if (incomingTime == null) {
            return false;
        }
        if (existingTime == null) {
            return true;
        }
        return !incomingTime.isBefore(existingTime);
    }

    private TestStatusRecord copy(TestStatusRecord source) {
        TestStatusRecord copy = new TestStatusRecord();
        copy.setId(source.getId());
        copy.setTaskId(source.getTaskId());
        copy.setUserId(source.getUserId());
        copy.setRunId(source.getRunId());
        copy.setStatus(source.getStatus());
        copy.setMessage(source.getMessage());
        copy.setCreatedAt(source.getCreatedAt());
        return copy;
    }

    private static class ExpiringRunRecord {
        private final TestStatusRecord record;
        private final LocalDateTime expireAt;

        private ExpiringRunRecord(TestStatusRecord record, LocalDateTime expireAt) {
            this.record = record;
            this.expireAt = expireAt;
        }

        public TestStatusRecord getRecord() {
            return record;
        }

        public LocalDateTime getExpireAt() {
            return expireAt;
        }
    }
}
