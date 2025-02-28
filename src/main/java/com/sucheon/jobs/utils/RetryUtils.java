package com.sucheon.jobs.utils;

import com.sucheon.jobs.config.UserProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
public class RetryUtils {

    private static final long MAX_SLEEP_MILLISECOND = 256 * 1000L;


    /**
     * 重试次数工具方法.
     *
     * @param callable 实际逻辑
     * @param retryTimes 最大重试次数（>1）
     * @param sleepTimeInMilliSecond 运行失败后休眠对应时间再重试
     * @param exponential 休眠时间是否指数递增
     * @param <T> 返回值类型
     * @return 经过重试的callable的执行结果
     */
    public static <T> T executeWithRetry(
            Callable<T> callable,
            int retryTimes,
            long sleepTimeInMilliSecond,
            boolean exponential) {
        Retry retry = new Retry();

        return retry.doRetry(callable, retryTimes, sleepTimeInMilliSecond, exponential, null);
    }


    private static class Retry {

        public <T> T doRetry(
                Callable<T> callable,
                int retryTimes,
                long sleepTimeInMilliSecond,
                boolean exponential,
                List<Class<?>> retryExceptionClasss) {

            if (null == callable) {
                throw new IllegalArgumentException("系统编程错误, 入参callable不能为空 ! ");
            }

            if (retryTimes < 1) {
                throw new IllegalArgumentException(
                        String.format("系统编程错误, 入参retrytime[%d]不能小于1 !", retryTimes));
            }

            Exception saveException = null;
            for (int i = 0; i < retryTimes; i++) {
                try {
                    return call(callable);
                } catch (Exception e) {
                    saveException = e;
                    if (i == 0) {
                        log.error(
                                String.format(
                                        "Exception when calling callable, 异常Msg:%s",
                                        ExceptionUtil.getErrorMessage(saveException)),
                                saveException);
                    }

                    if (null != retryExceptionClasss && !retryExceptionClasss.isEmpty()) {
                        boolean needRetry = false;
                        for (Class<?> eachExceptionClass : retryExceptionClasss) {
                            if (eachExceptionClass == e.getClass()) {
                                needRetry = true;
                                break;
                            }
                        }
                        if (!needRetry) {
                            throw new RuntimeException(saveException);
                        }
                    }

                    if (i + 1 < retryTimes && sleepTimeInMilliSecond > 0) {
                        long startTime = System.currentTimeMillis();

                        long timeToSleep;
                        if (exponential) {
                            timeToSleep = sleepTimeInMilliSecond * (long) Math.pow(2, i);
                            if (timeToSleep >= MAX_SLEEP_MILLISECOND) {
                                timeToSleep = MAX_SLEEP_MILLISECOND;
                            }
                        } else {
                            timeToSleep = sleepTimeInMilliSecond;
                            if (timeToSleep >= MAX_SLEEP_MILLISECOND) {
                                timeToSleep = MAX_SLEEP_MILLISECOND;
                            }
                        }

                        try {
                            Thread.sleep(timeToSleep);
                        } catch (InterruptedException ignored) {
                        }

                        long realTimeSleep = System.currentTimeMillis() - startTime;

                        log.error(
                                String.format(
                                        "Exception when calling callable, 即将尝试执行第%s次重试.本次重试计划等待[%s]ms,实际等待[%s]ms, 异常Msg:[%s]",
                                        i + 1,
                                        timeToSleep,
                                        realTimeSleep,
                                        ExceptionUtil.getErrorMessage(e)));
                    }
                }
            }
            throw new RuntimeException(saveException);
        }

        protected <T> T call(Callable<T> callable) throws Exception {
            return callable.call();
        }
    }
}
