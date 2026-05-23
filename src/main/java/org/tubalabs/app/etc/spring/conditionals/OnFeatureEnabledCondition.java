package org.tubalabs.app.etc.spring.conditionals;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Locale;
import java.util.Map;

@Slf4j
public class OnFeatureEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context,
                           AnnotatedTypeMetadata metadata) {
        final Map<String, Object> attributes = metadata.getAnnotationAttributes(FeatureEnabled.class.getName());
        if (attributes == null) {
            return true;
        }
        final String featureName = (String) attributes.get("value");
        final String property = ("FEATURE_" + featureName + "_ENABLED");
        return context.getEnvironment().getProperty(
                property.toLowerCase(Locale.ROOT),
                Boolean.class,
                context.getEnvironment().getProperty(
                        property.toUpperCase(Locale.ROOT),
                        Boolean.class,
                        true
                )
        );
    }
}