package com.example.eventbusdemo.event;

import java.lang.reflect.Method;

public class SubscriberMethod {
    Method method;
    ThreadMode threadMode;
    Class<?> eventType;//参数的类型
    int priority;
    boolean sticky;
    String methodString;
    public SubscriberMethod(Method method,Class<?> eventType,ThreadMode threadMode,int priority,boolean sticky){
        this.method = method;
        this.threadMode = threadMode;
        this.eventType = eventType;
        this.priority = priority;
        this.sticky = sticky;
    }
}
