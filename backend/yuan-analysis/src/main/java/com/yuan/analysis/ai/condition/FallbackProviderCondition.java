package com.yuan.analysis.ai.condition;

import com.yuan.analysis.ai.config.AnalysisAiProviders;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class FallbackProviderCondition extends AbstractAiProviderCondition {

    @Override
    public boolean matches(org.springframework.context.annotation.ConditionContext context, AnnotatedTypeMetadata metadata) {
        // provider 缺省时默认启用 fallback，保证 analysis 主链不会因为配置缺失而失效。
        return matchesProvider(context, AnalysisAiProviders.FALLBACK, true);
    }
}
