package com.spring_boot.school_department.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("within(com.spring_boot.school_department.controller..*)")
    public void controllerLayer() {
    }

    @Pointcut("within(com.spring_boot.school_department.service..*)")
    public void serviceLayer() {
    }

    @Around("controllerLayer() || serviceLayer()")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {

        String method = pjp.getSignature().getDeclaringType().getSimpleName() + "." + pjp.getSignature().getName();

        boolean controller = pjp.getSignature().getDeclaringTypeName().contains(".controller.");

        write(controller, "--> " + method + " args=" + Arrays.toString(pjp.getArgs()));

        long start = System.currentTimeMillis();

        try {

            Object result = pjp.proceed();

            write(controller, "<-- " + method + " (" + (System.currentTimeMillis() - start) + " ms)");

            return result;
        } catch (Throwable ex) {
            // stack trace global exception handler yazacaq
            write(controller, "<x- " + method + " (" + (System.currentTimeMillis() - start) + " ms) "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage());

            // global exception handler tutacaq tekrar
            throw ex;
        }
    }

    private void write(boolean controller, String message) {

        if (controller) {
            log.info(message);
        } else {
            log.debug(message);
        }
    }
}
