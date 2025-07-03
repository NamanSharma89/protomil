package com.protomil.core.shared.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// com/protomil/core/shared/logging/LogExecutionTime.java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecutionTime {
    String value() default "";
}