package com.yuan.analysis.llm;

import com.yuan.analysis.ai.condition.SeedCompatibleProviderCondition;
import com.yuan.analysis.ai.model.AiAnalysisInput;
import com.yuan.analysis.ai.model.AiAnalysisResult;
import com.yuan.analysis.dto.LlmAnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Conditional(SeedCompatibleProviderCondition.class)
public class SpringAiLlmClient implements LlmClient {

    private final ChatClient chatClient;
    private final String configuredModel;

    public SpringAiLlmClient(
            ChatClient.Builder builder,
            @Value("${spring.ai.openai.chat.options.model:unknown-model}") String configuredModel
    ) {
        this.chatClient = builder.build();
        this.configuredModel = configuredModel;
    }

    @Override
    public AiAnalysisResult analyze(String prompt, AiAnalysisInput input) {
        try {
            LlmAnalysisResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(LlmAnalysisResponse.class);

            return mapToAiResult(response);
        } catch (Exception e) {
            // AI 是增强层，调用失败时回退到规则结果，不中断整份分析报告。
            log.warn(
                    "Seed/OpenAI compatible analysis failed, fallback to rule-only result, taskId={}, runId={}, model={}",
                    input == null ? null : input.getTaskId(),
                    input == null ? null : input.getRunId(),
                    configuredModel,
                    e
            );
            return null;
        }
    }

    private AiAnalysisResult mapToAiResult(LlmAnalysisResponse response) {
        if (response == null) {
            return null;
        }

        AiAnalysisResult aiAnalysisResult = new AiAnalysisResult();
        aiAnalysisResult.setSummary(response.getSummary());
        aiAnalysisResult.setScoreAdjustment(response.getScoreAdjustment());
        aiAnalysisResult.setRationale(response.getRationale());
        // 这里记录的是实际配置的模型名。
        aiAnalysisResult.setModel(configuredModel);

        // 结构化输出字段允许缺失，保留默认空列表即可，避免后续 merge 时空指针。
        if (response.getBottlenecks() != null) {
            aiAnalysisResult.setBottlenecks(response.getBottlenecks());
        }
        if (response.getSuggestions() != null) {
            aiAnalysisResult.setSuggestions(response.getSuggestions());
        }
        return aiAnalysisResult;
    }
}
