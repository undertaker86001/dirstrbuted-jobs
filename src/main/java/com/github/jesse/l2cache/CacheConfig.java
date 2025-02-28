package com.github.jesse.l2cache;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.github.jesse.l2cache.consts.CacheType;
import com.github.jesse.l2cache.consts.HotkeyType;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

public class CacheConfig implements Serializable {

    private String instanceId = "C" + IdUtil.getSnowflakeNextIdStr();
    private boolean allowNullValues = true;
    private long nullValueExpireTimeSeconds = 60L;
    private long nullValueMaxSize = 3000L;
    private long nullValueClearPeriodSeconds = 10L;
    private boolean dynamic = true;
    private String logLevel = "debug";
    private boolean useL1ReplaceL2ExpireTime = true;
    private String cacheType;
    private final CacheConfig.Composite composite;
    private final CacheConfig.Caffeine caffeine;
    private final CacheConfig.Guava guava;
    private final CacheConfig.Redis redis;
    private final CacheConfig.CacheSyncPolicy cacheSyncPolicy;
    private final CacheConfig.Hotkey hotKey;

    public CacheConfig() {
        this.cacheType = CacheType.COMPOSITE.name();
        this.composite = new CacheConfig.Composite();
        this.caffeine = new CacheConfig.Caffeine();
        this.guava = new CacheConfig.Guava();
        this.redis = new CacheConfig.Redis();
        this.cacheSyncPolicy = new CacheConfig.CacheSyncPolicy();
        this.hotKey = new CacheConfig.Hotkey();
    }

    public String getInstanceId() {
        return this.instanceId;
    }

    public boolean isAllowNullValues() {
        return this.allowNullValues;
    }

    public long getNullValueExpireTimeSeconds() {
        return this.nullValueExpireTimeSeconds;
    }

    public long getNullValueMaxSize() {
        return this.nullValueMaxSize;
    }

    public long getNullValueClearPeriodSeconds() {
        return this.nullValueClearPeriodSeconds;
    }

    public boolean isDynamic() {
        return this.dynamic;
    }

    public String getLogLevel() {
        return this.logLevel;
    }

    public boolean isUseL1ReplaceL2ExpireTime() {
        return this.useL1ReplaceL2ExpireTime;
    }

    public String getCacheType() {
        return this.cacheType;
    }

    public CacheConfig.Composite getComposite() {
        return this.composite;
    }

    public CacheConfig.Caffeine getCaffeine() {
        return this.caffeine;
    }

    public CacheConfig.Guava getGuava() {
        return this.guava;
    }

    public CacheConfig.Redis getRedis() {
        return this.redis;
    }

    public CacheConfig.CacheSyncPolicy getCacheSyncPolicy() {
        return this.cacheSyncPolicy;
    }

    public CacheConfig.Hotkey getHotKey() {
        return this.hotKey;
    }

    public CacheConfig setInstanceId(String instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    public CacheConfig setAllowNullValues(boolean allowNullValues) {
        this.allowNullValues = allowNullValues;
        return this;
    }

    public CacheConfig setNullValueExpireTimeSeconds(long nullValueExpireTimeSeconds) {
        this.nullValueExpireTimeSeconds = nullValueExpireTimeSeconds;
        return this;
    }

    public CacheConfig setNullValueMaxSize(long nullValueMaxSize) {
        this.nullValueMaxSize = nullValueMaxSize;
        return this;
    }

    public CacheConfig setNullValueClearPeriodSeconds(long nullValueClearPeriodSeconds) {
        this.nullValueClearPeriodSeconds = nullValueClearPeriodSeconds;
        return this;
    }

    public CacheConfig setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
        return this;
    }

    public CacheConfig setLogLevel(String logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public CacheConfig setUseL1ReplaceL2ExpireTime(boolean useL1ReplaceL2ExpireTime) {
        this.useL1ReplaceL2ExpireTime = useL1ReplaceL2ExpireTime;
        return this;
    }

    public CacheConfig setCacheType(String cacheType) {
        this.cacheType = cacheType;
        return this;
    }

    public String toString() {
        return "CacheConfig(instanceId=" + this.getInstanceId() + ", allowNullValues=" + this.isAllowNullValues() + ", nullValueExpireTimeSeconds=" + this.getNullValueExpireTimeSeconds() + ", nullValueMaxSize=" + this.getNullValueMaxSize() + ", nullValueClearPeriodSeconds=" + this.getNullValueClearPeriodSeconds() + ", dynamic=" + this.isDynamic() + ", logLevel=" + this.getLogLevel() + ", useL1ReplaceL2ExpireTime=" + this.isUseL1ReplaceL2ExpireTime() + ", cacheType=" + this.getCacheType() + ", composite=" + this.getComposite() + ", caffeine=" + this.getCaffeine() + ", guava=" + this.getGuava() + ", redis=" + this.getRedis() + ", cacheSyncPolicy=" + this.getCacheSyncPolicy() + ", hotKey=" + this.getHotKey() + ")";
    }

    public static class Hotkey implements CacheConfig.Config, Serializable {
        private String type;
        private final CacheConfig.Hotkey.JdHotkey jd;
        private final CacheConfig.Hotkey.SentinelHotkey sentinel;

        public Hotkey() {
            this.type = HotkeyType.NONE.name();
            this.jd = new CacheConfig.Hotkey.JdHotkey();
            this.sentinel = new CacheConfig.Hotkey.SentinelHotkey();
        }

        public String getType() {
            return this.type;
        }

        public CacheConfig.Hotkey.JdHotkey getJd() {
            return this.jd;
        }

        public CacheConfig.Hotkey.SentinelHotkey getSentinel() {
            return this.sentinel;
        }

        public CacheConfig.Hotkey setType(String type) {
            this.type = type;
            return this;
        }

        public String toString() {
            return "CacheConfig.Hotkey(type=" + this.getType() + ", jd=" + this.getJd() + ", sentinel=" + this.getSentinel() + ")";
        }

        public static class SentinelHotkey implements CacheConfig.Config, Serializable {
            private ParamFlowRule defaultRule;
            private List<ParamFlowRule> rules = new ArrayList();

            public SentinelHotkey() {
            }

            public ParamFlowRule getDefaultRule() {
                return this.defaultRule;
            }

            public List<ParamFlowRule> getRules() {
                return this.rules;
            }

            public CacheConfig.Hotkey.SentinelHotkey setDefaultRule(ParamFlowRule defaultRule) {
                this.defaultRule = defaultRule;
                return this;
            }

            public CacheConfig.Hotkey.SentinelHotkey setRules(List<ParamFlowRule> rules) {
                this.rules = rules;
                return this;
            }

            public String toString() {
                return "CacheConfig.Hotkey.SentinelHotkey(defaultRule=" + this.getDefaultRule() + ", rules=" + this.getRules() + ")";
            }
        }

        public static class JdHotkey implements CacheConfig.Config, Serializable {
            private String serviceName = "default";
            private String etcdUrl;

            public JdHotkey() {
            }

            public String getServiceName() {
                return this.serviceName;
            }

            public String getEtcdUrl() {
                return this.etcdUrl;
            }

            public CacheConfig.Hotkey.JdHotkey setServiceName(String serviceName) {
                this.serviceName = serviceName;
                return this;
            }

            public CacheConfig.Hotkey.JdHotkey setEtcdUrl(String etcdUrl) {
                this.etcdUrl = etcdUrl;
                return this;
            }

            public String toString() {
                return "CacheConfig.Hotkey.JdHotkey(serviceName=" + this.getServiceName() + ", etcdUrl=" + this.getEtcdUrl() + ")";
            }
        }
    }

    public static class CacheSyncPolicy implements CacheConfig.Config, Serializable {
        private String type;
        private String topic = "l2cache";
        private boolean async;
        private Properties props = new Properties();

        public CacheSyncPolicy() {
        }

        public String getType() {
            return this.type;
        }

        public String getTopic() {
            return this.topic;
        }

        public boolean isAsync() {
            return this.async;
        }

        public Properties getProps() {
            return this.props;
        }

        public CacheConfig.CacheSyncPolicy setType(String type) {
            this.type = type;
            return this;
        }

        public CacheConfig.CacheSyncPolicy setTopic(String topic) {
            this.topic = topic;
            return this;
        }

        public CacheConfig.CacheSyncPolicy setAsync(boolean async) {
            this.async = async;
            return this;
        }

        public CacheConfig.CacheSyncPolicy setProps(Properties props) {
            this.props = props;
            return this;
        }

        public String toString() {
            return "CacheConfig.CacheSyncPolicy(type=" + this.getType() + ", topic=" + this.getTopic() + ", async=" + this.isAsync() + ", props=" + this.getProps() + ")";
        }
    }

    public static class Redis implements CacheConfig.Config, Serializable {
        private boolean lock = false;
        private boolean tryLock = true;
        private long expireTime;
        private Map<String, Long> expireTimeCacheNameMap = new HashMap();
        private int batchPageSize = 50;
        private String batchGetLogLevel = "info";
        private String printDetailLogSwitch = "off";
        private String redissonYamlConfig;
        private org.redisson.config.Config redissonConfig;

        public Redis() {
        }

        public org.redisson.config.Config getRedissonConfig() {
            if (StrUtil.isBlank(this.redissonYamlConfig) && this.redissonConfig == null) {
                return null;
            } else if (null != this.redissonConfig) {
                return this.redissonConfig;
            } else {
                try {
                    InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(this.redissonYamlConfig);
                    if (null == is) {
                        throw new IllegalStateException("not found redisson yaml config file:" + this.redissonYamlConfig);
                    } else {
                        this.redissonConfig = org.redisson.config.Config.fromYAML(is);
                        return this.redissonConfig;
                    }
                } catch (IOException var2) {
                    throw new IllegalStateException("parse redisson yaml config error", var2);
                }
            }
        }

        public boolean isLock() {
            return this.lock;
        }

        public boolean isTryLock() {
            return this.tryLock;
        }

        public long getExpireTime() {
            return this.expireTime;
        }

        public Map<String, Long> getExpireTimeCacheNameMap() {
            return this.expireTimeCacheNameMap;
        }

        public int getBatchPageSize() {
            return this.batchPageSize;
        }

        public String getBatchGetLogLevel() {
            return this.batchGetLogLevel;
        }

        public String getPrintDetailLogSwitch() {
            return this.printDetailLogSwitch;
        }

        public String getRedissonYamlConfig() {
            return this.redissonYamlConfig;
        }

        public CacheConfig.Redis setLock(boolean lock) {
            this.lock = lock;
            return this;
        }

        public CacheConfig.Redis setTryLock(boolean tryLock) {
            this.tryLock = tryLock;
            return this;
        }

        public CacheConfig.Redis setExpireTime(long expireTime) {
            this.expireTime = expireTime;
            return this;
        }

        public CacheConfig.Redis setExpireTimeCacheNameMap(Map<String, Long> expireTimeCacheNameMap) {
            this.expireTimeCacheNameMap = expireTimeCacheNameMap;
            return this;
        }

        public CacheConfig.Redis setBatchPageSize(int batchPageSize) {
            this.batchPageSize = batchPageSize;
            return this;
        }

        public CacheConfig.Redis setBatchGetLogLevel(String batchGetLogLevel) {
            this.batchGetLogLevel = batchGetLogLevel;
            return this;
        }

        public CacheConfig.Redis setPrintDetailLogSwitch(String printDetailLogSwitch) {
            this.printDetailLogSwitch = printDetailLogSwitch;
            return this;
        }

        public CacheConfig.Redis setRedissonYamlConfig(String redissonYamlConfig) {
            this.redissonYamlConfig = redissonYamlConfig;
            return this;
        }

        public CacheConfig.Redis setRedissonConfig(org.redisson.config.Config redissonConfig) {
            this.redissonConfig = redissonConfig;
            return this;
        }

        public String toString() {
            return "CacheConfig.Redis(lock=" + this.isLock() + ", tryLock=" + this.isTryLock() + ", expireTime=" + this.getExpireTime() + ", expireTimeCacheNameMap=" + this.getExpireTimeCacheNameMap() + ", batchPageSize=" + this.getBatchPageSize() + ", batchGetLogLevel=" + this.getBatchGetLogLevel() + ", printDetailLogSwitch=" + this.getPrintDetailLogSwitch() + ", redissonYamlConfig=" + this.getRedissonYamlConfig() + ", redissonConfig=" + this.getRedissonConfig() + ")";
        }
    }

    public static class Guava implements CacheConfig.Config, Serializable {
        private boolean autoRefreshExpireCache = true;
        private Integer refreshPoolSize = Runtime.getRuntime().availableProcessors();
        private Long refreshPeriod = 30L;
        private String defaultSpec;
        private Map<String, String> specs = new HashMap();

        public Guava() {
        }

        public boolean isAutoRefreshExpireCache() {
            return this.autoRefreshExpireCache;
        }

        public Integer getRefreshPoolSize() {
            return this.refreshPoolSize;
        }

        public Long getRefreshPeriod() {
            return this.refreshPeriod;
        }

        public String getDefaultSpec() {
            return this.defaultSpec;
        }

        public Map<String, String> getSpecs() {
            return this.specs;
        }

        public CacheConfig.Guava setAutoRefreshExpireCache(boolean autoRefreshExpireCache) {
            this.autoRefreshExpireCache = autoRefreshExpireCache;
            return this;
        }

        public CacheConfig.Guava setRefreshPoolSize(Integer refreshPoolSize) {
            this.refreshPoolSize = refreshPoolSize;
            return this;
        }

        public CacheConfig.Guava setRefreshPeriod(Long refreshPeriod) {
            this.refreshPeriod = refreshPeriod;
            return this;
        }

        public CacheConfig.Guava setDefaultSpec(String defaultSpec) {
            this.defaultSpec = defaultSpec;
            return this;
        }

        public CacheConfig.Guava setSpecs(Map<String, String> specs) {
            this.specs = specs;
            return this;
        }

        public String toString() {
            return "CacheConfig.Guava(autoRefreshExpireCache=" + this.isAutoRefreshExpireCache() + ", refreshPoolSize=" + this.getRefreshPoolSize() + ", refreshPeriod=" + this.getRefreshPeriod() + ", defaultSpec=" + this.getDefaultSpec() + ", specs=" + this.getSpecs() + ")";
        }
    }

    public static class Caffeine implements CacheConfig.Config, Serializable {
        private boolean autoRefreshExpireCache = false;
        private Integer refreshPoolSize = Runtime.getRuntime().availableProcessors();
        private Long refreshPeriod = 30L;
        private Long publishMsgPeriodMilliSeconds = 500L;
        private String batchGetLogLevel = "debug";
        private String defaultSpec;
        private Map<String, String> specs = new HashMap();
        private boolean enableMdcForkJoinPool = true;

        public Caffeine() {
        }

        public boolean isAutoRefreshExpireCache() {
            return this.autoRefreshExpireCache;
        }

        public Integer getRefreshPoolSize() {
            return this.refreshPoolSize;
        }

        public Long getRefreshPeriod() {
            return this.refreshPeriod;
        }

        public Long getPublishMsgPeriodMilliSeconds() {
            return this.publishMsgPeriodMilliSeconds;
        }

        public String getBatchGetLogLevel() {
            return this.batchGetLogLevel;
        }

        public String getDefaultSpec() {
            return this.defaultSpec;
        }

        public Map<String, String> getSpecs() {
            return this.specs;
        }

        public boolean isEnableMdcForkJoinPool() {
            return this.enableMdcForkJoinPool;
        }

        public CacheConfig.Caffeine setAutoRefreshExpireCache(boolean autoRefreshExpireCache) {
            this.autoRefreshExpireCache = autoRefreshExpireCache;
            return this;
        }

        public CacheConfig.Caffeine setRefreshPoolSize(Integer refreshPoolSize) {
            this.refreshPoolSize = refreshPoolSize;
            return this;
        }

        public CacheConfig.Caffeine setRefreshPeriod(Long refreshPeriod) {
            this.refreshPeriod = refreshPeriod;
            return this;
        }

        public CacheConfig.Caffeine setPublishMsgPeriodMilliSeconds(Long publishMsgPeriodMilliSeconds) {
            this.publishMsgPeriodMilliSeconds = publishMsgPeriodMilliSeconds;
            return this;
        }

        public CacheConfig.Caffeine setBatchGetLogLevel(String batchGetLogLevel) {
            this.batchGetLogLevel = batchGetLogLevel;
            return this;
        }

        public CacheConfig.Caffeine setDefaultSpec(String defaultSpec) {
            this.defaultSpec = defaultSpec;
            return this;
        }

        public CacheConfig.Caffeine setSpecs(Map<String, String> specs) {
            this.specs = specs;
            return this;
        }

        public CacheConfig.Caffeine setEnableMdcForkJoinPool(boolean enableMdcForkJoinPool) {
            this.enableMdcForkJoinPool = enableMdcForkJoinPool;
            return this;
        }

        public String toString() {
            return "CacheConfig.Caffeine(autoRefreshExpireCache=" + this.isAutoRefreshExpireCache() + ", refreshPoolSize=" + this.getRefreshPoolSize() + ", refreshPeriod=" + this.getRefreshPeriod() + ", publishMsgPeriodMilliSeconds=" + this.getPublishMsgPeriodMilliSeconds() + ", batchGetLogLevel=" + this.getBatchGetLogLevel() + ", defaultSpec=" + this.getDefaultSpec() + ", specs=" + this.getSpecs() + ", enableMdcForkJoinPool=" + this.isEnableMdcForkJoinPool() + ")";
        }
    }

    public static class Composite implements CacheConfig.Config, Serializable {
        private String l1CacheType;
        private String l2CacheType;
        private boolean l2BatchPut;
        private boolean l2BatchEvict;
        private boolean l1AllOpen;
        private boolean l1Manual;
        private Set<String> l1ManualKeySet;
        private Set<String> l1ManualCacheNameSet;

        public Composite() {
            this.l1CacheType = CacheType.CAFFEINE.name();
            this.l2CacheType = CacheType.REDIS.name();
            this.l2BatchPut = false;
            this.l2BatchEvict = false;
            this.l1AllOpen = false;
            this.l1Manual = false;
            this.l1ManualKeySet = new HashSet();
            this.l1ManualCacheNameSet = new HashSet();
        }

        public String getL1CacheType() {
            return this.l1CacheType;
        }

        public String getL2CacheType() {
            return this.l2CacheType;
        }

        public boolean isL2BatchPut() {
            return this.l2BatchPut;
        }

        public boolean isL2BatchEvict() {
            return this.l2BatchEvict;
        }

        public boolean isL1AllOpen() {
            return this.l1AllOpen;
        }

        public boolean isL1Manual() {
            return this.l1Manual;
        }

        public Set<String> getL1ManualKeySet() {
            return this.l1ManualKeySet;
        }

        public Set<String> getL1ManualCacheNameSet() {
            return this.l1ManualCacheNameSet;
        }

        public CacheConfig.Composite setL1CacheType(String l1CacheType) {
            this.l1CacheType = l1CacheType;
            return this;
        }

        public CacheConfig.Composite setL2CacheType(String l2CacheType) {
            this.l2CacheType = l2CacheType;
            return this;
        }

        public CacheConfig.Composite setL2BatchPut(boolean l2BatchPut) {
            this.l2BatchPut = l2BatchPut;
            return this;
        }

        public CacheConfig.Composite setL2BatchEvict(boolean l2BatchEvict) {
            this.l2BatchEvict = l2BatchEvict;
            return this;
        }

        public CacheConfig.Composite setL1AllOpen(boolean l1AllOpen) {
            this.l1AllOpen = l1AllOpen;
            return this;
        }

        public CacheConfig.Composite setL1Manual(boolean l1Manual) {
            this.l1Manual = l1Manual;
            return this;
        }

        public CacheConfig.Composite setL1ManualKeySet(Set<String> l1ManualKeySet) {
            this.l1ManualKeySet = l1ManualKeySet;
            return this;
        }

        public CacheConfig.Composite setL1ManualCacheNameSet(Set<String> l1ManualCacheNameSet) {
            this.l1ManualCacheNameSet = l1ManualCacheNameSet;
            return this;
        }

        public String toString() {
            return "CacheConfig.Composite(l1CacheType=" + this.getL1CacheType() + ", l2CacheType=" + this.getL2CacheType() + ", l2BatchPut=" + this.isL2BatchPut() + ", l2BatchEvict=" + this.isL2BatchEvict() + ", l1AllOpen=" + this.isL1AllOpen() + ", l1Manual=" + this.isL1Manual() + ", l1ManualKeySet=" + this.getL1ManualKeySet() + ", l1ManualCacheNameSet=" + this.getL1ManualCacheNameSet() + ")";
        }
    }

    public interface Config {
    }
}
