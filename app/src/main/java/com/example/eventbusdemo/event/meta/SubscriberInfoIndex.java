package com.example.eventbusdemo.event.meta;

public interface SubscriberInfoIndex {
    SubscriberInfo getSubscriberInfo(Class<?> subscriberClass);
}
