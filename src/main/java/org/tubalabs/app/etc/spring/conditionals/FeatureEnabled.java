package org.tubalabs.app.etc.spring.conditionals;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnFeatureEnabledCondition.class)
public @interface FeatureEnabled {

    String value();

}