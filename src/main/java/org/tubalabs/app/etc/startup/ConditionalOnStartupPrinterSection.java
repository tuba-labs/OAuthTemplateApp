package org.tubalabs.app.etc.startup;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(prefix = "startupprinter", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnStartupPrinterSection {
    @AliasFor(annotation = ConditionalOnProperty.class, attribute = "name")
    String[] name();
}
