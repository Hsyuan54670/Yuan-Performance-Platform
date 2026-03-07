import { Card, Collapse, Tag, Typography } from "antd";
import { useTranslation } from "react-i18next";
import type { SuggestionItem } from "../types/analysis";

interface OptimizationSuggestionListProps {
  items: SuggestionItem[];
}

const priorityColor: Record<string, string> = {
  P0: "red",
  P1: "orange",
  P2: "blue"
};

function OptimizationSuggestionList({ items }: OptimizationSuggestionListProps) {
  const { t } = useTranslation();

  return (
    <Card className="glass-card" title={t("components.optimizationSuggestions.title")}>
      <Collapse
        accordion
        items={items.map((item) => ({
          key: String(item.id),
          label: (
            <span>
              <Tag color={priorityColor[item.priority]}>{item.priority}</Tag>
              <Typography.Text strong>{item.title}</Typography.Text>
            </span>
          ),
          children: <Typography.Text type="secondary">{item.detail}</Typography.Text>
        }))}
      />
    </Card>
  );
}

export default OptimizationSuggestionList;
