package com.sucheon.jobs.constant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sucheon.jobs.utils.JsonConfigUtils;
import com.sucheon.jobs.utils.QueueOperatorUtils;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import java.util.ArrayList;
import java.util.List;

public class CommonConstant {

    public static final ObjectMapper objectMapper = JsonConfigUtils.getObjectMapper();

    public static final CloseableHttpAsyncClient httpClient = HttpAsyncClients.createDefault();

    public static final QueueOperatorUtils queueOperatorUtils = new QueueOperatorUtils();

    public static final String algSinkTopic = "ck_alg_main";

    public static final String iotSinkTopic = "ck_iot_main";

    public static final String alarmTopic = "scpc.alarm";

    public static final String algComputeTopic = "scpc.alg_xxxx";

    public static final String algLabel = "alg-data";

    public static final String iotLabel = "iot-data";
}
