package com.sucheon.jobs.constant;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RedisConstant {

    /**
     * iot入库ck的redis配置 (需要配置该点位数据的不入库字段)
     */
    public static final String IOT_NOT_STORE_FIELDS = "scpc:tms:fieldTemplate";

    /**
     * iot测点  终端管理服务-点位树(只需要更新点位和不入库模版之间的关系)
     */
    public static final String IOT_POINT_TREE = "scpc:tms:pointTree";

    /**
     * 分发到告警的数据
     */
    public static final String ALARM_RULE = "scpc:alarm:rule";

    /**
     * 算法实例和数据源的变更关系
     */
    public static final String ALG_INSTANCE_RULE = "scpc:alg:job";


    public List<String> allChannelNames() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<String> allChannelNames = new ArrayList<>();
        Class<? extends RedisConstant> tClass = this.getClass();
        Object obj = tClass.getConstructor().newInstance();
        Field[] declaredFields = tClass.getDeclaredFields();
        //遍历字段
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);
            Object value = declaredField.get(obj);
            if (!Objects.isNull(value)) {
                allChannelNames.add(String.valueOf(value));
            }
        }
        return allChannelNames;
    }

    public static void main(String[] args) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        RedisConstant redisConstant = new RedisConstant();
        System.out.println(redisConstant.allChannelNames());
    }

}
