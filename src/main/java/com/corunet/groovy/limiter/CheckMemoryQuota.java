package com.corunet.groovy.limiter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

/**
 * Adds memory quota checks to a scripts execution.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.PACKAGE, ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.LOCAL_VARIABLE})
@GroovyASTTransformationClass("com.corunet.groovy.limiter.CheckMemoryQuotaASTTransformation")
public @interface CheckMemoryQuota {

    /**
     * Memory limit in bytes
     */
    long limit();
    /**
     * Handler class name
     */
    Class<?> handlerClass();
    /**
     * Handler method name
     */
    String handlerMethod();
}
