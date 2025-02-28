package com.sucheon.jobs.config;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

@Getter
@Builder
public class HttpConf implements Serializable {

    private Integer retryTimes;

    private Integer sleepTimeInMilliSecond;

    private Boolean exponential;
}
