package com.sucheon.jobs.constant;

public class HttpConstant {

    /**
     * 终端管理服务(点位树更新配置获取接口)
     */
    public static final String iotUpdateHttpUrl = "/scpc/tms/PointTree/2342423422/update";

    /**
     * 算法不入库字段(通过点位更新接口)
     */
    public static final String algNotStoreUpdateHttpUrl = "/alg/sync/jobs/instances/notStoreFields";


    /**
     * 算法实例与数据源绑定关系更新接口
     */
    public static final String algInstanceAndDataSourceUpdateUrl = "/alg/sync/jobs/instances/dataSources";
}
