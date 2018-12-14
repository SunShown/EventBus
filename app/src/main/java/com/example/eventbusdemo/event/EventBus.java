package com.example.eventbusdemo.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    static EventBus defaultInstance;
    private static final EventBusBuilder DEFAULT_BUILDER = new EventBusBuilder();
//    private static final Map<Class<?>, CopyOnWriteArrayList<SubscriberMethod>>

    private final SubscriberMethodFinder subscriberMethodFinder = null;
    public static EventBus getDefault(){
        if (defaultInstance == null){
            synchronized (EventBus.class){
                if (defaultInstance == null){
                    defaultInstance = new EventBus();
                }
            }
        }
        return defaultInstance;
    }

    public EventBus(){
        this(DEFAULT_BUILDER);
    }
    public EventBus(EventBusBuilder busBuilder){
//        subscriberMethodFinder = new SubscriberMethodFinder();
    }
    public void register(Object subscriber){
        Class<?> subscriberClass = subscriber.getClass();
//        List<SubscriberMethod> subscriberMethods =
    }
}
