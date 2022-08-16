package com.adong.fingermark.manager;

import com.adong.fingermark.config.PropertyFactory;
import com.adong.fingermark.constant.ConfigConstant;
import com.adong.fingermark.core.IdGen;
import com.adong.fingermark.core.SnowFlakeId;
import com.adong.fingermark.core.WorkerIdManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author ADong
 * @Description IdGenManager
 * @Date 2022-08-15 5:36 PM
 */
@Component
public class IdGenManager implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(IdGenManager.class);

    @Autowired
    private RedisTemplate redisTemplate;

    private final ConcurrentHashMap<String, IdGen> registers = new ConcurrentHashMap<>();

    private IdGen DEFAULT_IDGEN;

    /**
     * 生成id默认方法
     * @return
     */
    public long getId() {

        return DEFAULT_IDGEN.nextId();
    }

    /**
     * 根据 appKey 生成 id
     * @param appKey
     * @return
     */
    public long getId(String appKey) {

        return registers.getOrDefault(appKey, DEFAULT_IDGEN).nextId();
    }

    public void registerAppKey(String appKey) {

        registers.putIfAbsent(appKey, register(appKey));
    }

    public void removeAppKey(String appKey) {
        IdGen idGen = registers.get(appKey);
        idGen.destory();
        registers.remove(appKey);
    }

    /**
     * 注册雪花算法 ID 生成器
     * @param appKey
     * @return
     */
    private IdGen register(String appKey) {

        Properties properties = PropertyFactory.getProperties();
        long workerIdShift = Long.parseLong(properties.getProperty(ConfigConstant.WORKERID_SHIFT, "10"));
        boolean isOpen = Boolean.parseBoolean(properties.getProperty(ConfigConstant.OPEN_SEQUENCE_SET_WORKERID, "true"));
        WorkerIdManager workerIdManager = new WorkerIdManager(redisTemplate, isOpen, appKey);
        Long workerId = null;
        try {
            workerId = workerIdManager.registerAndGetWorkerId();
        } catch (Exception e) {
            log.error("workerIdManager getWorkerId error", e);
        }
        // 降级方案
        if (workerId == null) {
            workerId = ThreadLocalRandom.current().nextLong(0, (long) Math.pow(2, workerIdShift));
            log.info("降级方案 WorkerId={}", workerId);
        }
        return new SnowFlakeId(workerId, workerIdShift, workerIdManager);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        DEFAULT_IDGEN = register("default");
    }
}
