import { Card, Tag, Timeline, Typography } from "antd";
import { useTranslation } from "react-i18next";
import type { BottleneckItem } from "../types/analysis";

interface BottleneckTimelineProps {
  items: BottleneckItem[];
}

const colorMap = {
  LOW: "green",
  MEDIUM: "blue",
  HIGH: "orange",
  CRITICAL: "red"
};

function BottleneckTimeline({ items }: BottleneckTimelineProps) {
  const { t } = useTranslation();

  return (
    <Card className="glass-card" title={t("components.bottleneckTimeline.title")}>
      <Timeline
        items={items.map((item) => ({
          color: item.severity === "CRITICAL" ? "red" : item.severity === "HIGH" ? "orange" : "blue",
          children: (
            <div>
              <Typography.Text strong>{item.time}</Typography.Text>
              <div style={{ margin: "6px 0" }}>
                <Tag color="cyan">{t(`components.bottleneckType.${item.type}`)}</Tag>
                <Tag color={colorMap[item.severity]}>{t(`components.severity.${item.severity}`)}</Tag>
              </div>
              <Typography.Text type="secondary">{item.reason}</Typography.Text>
            </div>
          )
        }))}
      />
    </Card>
  );
}

export default BottleneckTimeline;
