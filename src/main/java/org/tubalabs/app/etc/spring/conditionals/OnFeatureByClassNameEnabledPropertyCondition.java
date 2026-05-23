package org.tubalabs.app.etc.spring.conditionals;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.util.ClassUtils;

@Slf4j
public class OnFeatureByClassNameEnabledPropertyCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        if (!(metadata instanceof ClassMetadata classMetadata)) {
            log.warn("Condition only supports class metadata: {}", metadata.getClass().getName());
            return true;
        }
        final String shortName = ClassUtils.getShortName(classMetadata.getClassName());
        final String property = "feature_" + shortName + "_enabled";
        return context.getEnvironment().getProperty(property.toUpperCase(), Boolean.class, true);
    }
}