package com.yuan.analysis.ai.condition;

import com.yuan.analysis.ai.config.AnalysisAiProviders;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class SeedCompatibleProviderCondition extends AbstractAiProviderCondition {

    @Override
    public boolean matches(org.springframework.context.annotation.ConditionContext context, AnnotatedTypeMetadata metadata) {
        return matchesProvider(context, AnalysisAiProviders.SEED_COMPATIBLE, false);
    }
}
