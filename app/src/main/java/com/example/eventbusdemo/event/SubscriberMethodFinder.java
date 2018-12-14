package com.example.eventbusdemo.event;

import com.example.eventbusdemo.event.meta.SubscriberInfo;
import com.example.eventbusdemo.event.meta.SubscriberInfoIndex;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class SubscriberMethodFinder {
    private static final  int BRIDGE = 0x40;
    private static final int SYNTHTIC = 0x1000;
    private static final int MODIFIERS_IGNORE  =Modifier.ABSTRACT | Modifier.STATIC |BRIDGE |SYNTHTIC;
    private static final Map<Class<?>,List<SubscriberMethod>> METHOD_CACHE = new ConcurrentHashMap<>();

    private static final int POOL_SIZE = 4;
    private static final FindState[] FIND_STATES_POOL = new FindState[POOL_SIZE];

    private final boolean strictMethodVerification;
    private final boolean ignoreGeneratedIndex ;
    private List<SubscriberInfoIndex> subscriberInfoIndexs;

    SubscriberMethodFinder(List<SubscriberInfoIndex> subscriberInfoIndexs,boolean strictMethodVerification,
                           boolean ignoreGeneratedIndex){
        this.subscriberInfoIndexs = subscriberInfoIndexs;
        this.strictMethodVerification = strictMethodVerification;
        this.ignoreGeneratedIndex = ignoreGeneratedIndex;
    }

    List<SubscriberMethod> findSubscriberMethods(Class<?> subscirberClass){
        List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscirberClass);
        if (subscriberMethods != null){
            return subscriberMethods;
        }
        subscriberMethods = findUsingInfo(subscirberClass);
        if (!subscriberMethods.isEmpty()){
            METHOD_CACHE.put(subscirberClass,subscriberMethods);
            return subscriberMethods;
        }else {
            throw new RuntimeException("未找到该类和父类@Subscribe 注解的public 方法");
        }
    }


    private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass){
        FindState findState = prepareFindState();
        findState.initForSubscriber(subscriberClass);
        while (findState.clazz != null){
            findState.subscriberInfo = getSubscriberInfo(findState);
            if (findState.subscriberInfo != null){
                SubscriberMethod[] array = findState.subscriberInfo.getSubscriberMethods();
                for (SubscriberMethod subscriberMethod : array) {
                    if (findState.checkAdd(subscriberMethod.method,subscriberMethod.eventType)){
                        findState.subscriberMethods.add(subscriberMethod);
                    }
                }
            }else {
                findUsingReflectionInSingleClass(findState);
            }
        }
        return null;
    }

    private FindState prepareFindState(){
        for (int i = 0; i < POOL_SIZE; i++) {
            FindState state = FIND_STATES_POOL[i];
            if (state != null){
                FIND_STATES_POOL[i] = null;
                return state;
            }
        }
        return new FindState();
    }

    private SubscriberInfo getSubscriberInfo(FindState findState){
        if (findState.subscriberInfo != null && findState.subscriberInfo.getSuperSubscriberInfo() != null){
            SubscriberInfo superclassInfo = findState.subscriberInfo.getSuperSubscriberInfo();
            if (findState.clazz == superclassInfo.getSubscribeClass()){
                return superclassInfo;
            }
        }
        if (subscriberInfoIndexs != null){
            for (SubscriberInfoIndex index : subscriberInfoIndexs) {
                SubscriberInfo info = index.getSubscriberInfo(findState.clazz);
                if (info != null){
                    return info;
                }
            }
        }
        return null;
    }
    static class FindState {
        List<SubscriberMethod> subscriberMethods = new ArrayList<>();
        Map<Class,Object> anyMethodByEvnetType = new HashMap<>();
        Map<String,Class> subscriberClassByMethodKey = new HashMap<>();
        StringBuilder methodKeyBuilder = new StringBuilder(128);

        Class<?> subscirberClass;
        Class<?> clazz;
        boolean skipSuperClasses;
        SubscriberInfo subscriberInfo ;
        void initForSubscriber(Class<?> subscriberClass){
            this.subscirberClass = clazz = subscriberClass;
            skipSuperClasses = false;
            subscriberInfo = null;
        }

        /**
         *
         * @param method 方法
         * @param evnetType 方法的首参数类型
         * @return
         */
        boolean checkAdd(Method method,Class<?> evnetType){
            //两层查找 1st by eventType
            Object existing = anyMethodByEvnetType.put(evnetType,method);
            //相同的key,第一次存放直接返回
            if (existing == null){
                return true;
            }else {
                //该参数类型之前已经存过
                //当查找到一个类中接受了两个一样的Event
                if (existing instanceof Method){
                    //2st by 方法签名（方法名+参数）
                    if (!checkAddWithMethodSignature((Method) existing,evnetType)){
                        throw new IllegalStateException();
                    }
                    anyMethodByEvnetType.put(evnetType,this);
                }
                return checkAddWithMethodSignature(method,evnetType);
            }
        }

        void moveToSupercl(){

        };
        private boolean checkAddWithMethodSignature(Method method,Class<?> eventType){
            methodKeyBuilder.setLength(0);
            methodKeyBuilder.append(method.getName());
            methodKeyBuilder.append('>').append(eventType.getName());
            //将方法签名作为key
            String methodKey = methodKeyBuilder.toString();
            Class<?> methodClass = method.getDeclaringClass();

            Class<?> methodClassOld = subscriberClassByMethodKey.put(methodKey,methodClass);//返回之前的 methodClass
            if (methodClassOld == null || methodClassOld.isAssignableFrom(methodClass)){
                //同一个event
                return true;
            }else {
                //什么情况下会走这里呢
                subscriberClassByMethodKey.put(methodKey,methodClassOld);
                return false;
            }
        }
    }

    private void findUsingReflectionInSingleClass(FindState findState){
        Method[] methods;
        try {
            //只找寻当前类中的方法，不包括继承方法
            methods = findState.clazz.getDeclaredMethods();
        }catch (Throwable th){
            methods = findState.clazz.getMethods();
            findState.skipSuperClasses = true;
        }
        for (Method method : methods) {
            int modifiers = method.getModifiers();
            if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0){
                //必须为public，no_abstract ,no_static.，为啥不能访问私有呢，估计设计之初就是怕破坏Java代码。
                Class<?>[] parameterTypes = method.getParameterTypes();
                //1st,找到所有符合上述条件的方法
                if (parameterTypes.length == 1){
                    Subscribe subscribeAnnotaion = method.getAnnotation(Subscribe.class);
                    //2st,找到有注解的方法
                    if (subscribeAnnotaion != null){
                        //参数类型
                        Class<?> eventType = parameterTypes[0];

                        if (findState.checkAdd(method,eventType)){
                            ThreadMode threadMode = subscribeAnnotaion.threadMode();
                            findState.subscriberMethods.add(new SubscriberMethod(method,eventType,threadMode,
                                    subscribeAnnotaion.priority(),subscribeAnnotaion.sticky()));
                        }
                    }
                }else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)){
                    String methodName = method.getDeclaringClass().getName() +"."+method.getName();
                    throw new RuntimeException("@Subscirbe 注解方法必须只能包含一个参数");
                }
            }else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)){
                throw new RuntimeException("必须为public，no_abstract ,no_static.");
            }
        }
    }
}
