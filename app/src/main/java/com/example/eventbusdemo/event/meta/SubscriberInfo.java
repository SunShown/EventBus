package com.example.eventbusdemo.event.meta;

import com.example.eventbusdemo.event.SubscriberMethod;

public interface SubscriberInfo {
    Class<?> getSubscribeClass();
    SubscriberMethod[] getSubscriberMethods();
    SubscriberInfo getSuperSubscriberInfo();
    boolean shouldCheckSuperclass();
}
