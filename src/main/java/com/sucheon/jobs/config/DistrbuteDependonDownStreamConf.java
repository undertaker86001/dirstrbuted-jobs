package com.sucheon.jobs.config;

import lombok.Builder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;

import java.io.Serializable;

/**
 * 是否需要分发到下游开关
 */
@Getter
@Builder
public class DistrbuteDependonDownStreamConf implements Serializable {


    /**
     * 算法链路分发开关
     */
    private Boolean isAlgOpen;


    /**
     * 告警链路分发开关
     */
    private Boolean isAlarmOpen;

    /**
     * ck入库分发开关
     */
    private Boolean isStoreOpen;
}
