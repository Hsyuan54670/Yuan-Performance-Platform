package com.yuan.analysis.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "yuan.analysis.ai")
public class AnalysisAiProperties {

    /** 是否启用 AI 增强层 */
    private boolean enabled = true;

    /** provider 统一从这里收口，便于后续扩展 openai / seed / fallback 等实现 */
    private String provider = AnalysisAiProviders.FALLBACK;

    /** AI 允许对规则基准分做的最大调整幅度 */
    private int maxScoreAdjustment = 10;
}
