package com.sucheon.jobs.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 进行多分发端的规则计算后，推送到下游的数据实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DynamicKeyedBean {

    /**
     * 来源于边缘端还是算法端的数据
     */
    private String origin;

    /**
     * 多个code的点位书编码 code1, code2, code3....
     */
    private String codeList;

    /**
     * 点位数据(边缘端投递到下游使用)
     */
    private String pointData;

    /**
     * 当前要执行的算法实例
     */
    private String instanceId;


    /**
     * 算法实例数据(用户在创建场景树之后，算法侧需要回传兼容的数据)
     */
    private AlgConf algConf;

    /**
     * 推送到哪个终端
     */
    private String topic;

    /**
     * 输出的算法归属于哪个算法组
     */
    private String algGroup;



    /**
     * 枚举类实现单例模式，保证线程安全
     */
    static enum DynamicKeyedBeanSingleton{
        INSTANCE;
        private DynamicKeyedBean dynamicKeyedBean;
        DynamicKeyedBeanSingleton(){
            dynamicKeyedBean = new DynamicKeyedBean();
        }

        public DynamicKeyedBean getInstance(){
            return dynamicKeyedBean;
        }
    };

    public static DynamicKeyedBean getInstance(){
        return DynamicKeyedBean.DynamicKeyedBeanSingleton.INSTANCE.getInstance();
    }
}
