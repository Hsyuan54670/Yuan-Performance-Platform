package com.yuan.analysis.ai.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

abstract class AbstractAiProviderCondition implements Condition {

    private static final String PROVIDER_KEY = "yuan.analysis.ai.provider";

    protected boolean matchesProvider(ConditionContext context, String expectedProvider, boolean matchIfMissing) {
        String configuredProvider = context.getEnvironment().getProperty(PROVIDER_KEY);
        if (!StringUtils.hasText(configuredProvider)) {
            return matchIfMissing;
        }
        return expectedProvider.equalsIgnoreCase(configuredProvider.trim());
    }

    @Override
    public abstract boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);
}
