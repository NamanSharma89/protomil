package com.protomil.core.shared.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("@annotation(logExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();

        MDC.put("method", methodName);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            log.info("Method {} executed successfully in {} ms", methodName, duration);
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Method {} failed after {} ms", methodName, duration, e);
            throw e;
        } finally {
            MDC.remove("method");
        }
    }
}
