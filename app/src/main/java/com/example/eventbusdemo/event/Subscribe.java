package com.example.eventbusdemo.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscribe {
    ThreadMode threadMode() default ThreadMode.POSTING;

    boolean sticky() default false;
    int priority() default 0;
}
