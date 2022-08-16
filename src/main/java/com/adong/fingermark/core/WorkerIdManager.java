package com.adong.fingermark.core;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author ADong
 * @Description WorkerId 管理，{@link SnowFlakeId#workerId}
 * @Date 2022-08-15 11:45 AM
 */
public class WorkerIdManager {

    private static final Logger log = LoggerFactory.getLogger(WorkerIdManager.class);

    private final RedisTemplate redisTemplate;

    private final String appKey;

    private final boolean openSequenceSetWorkerId;

    private final ScheduledExecutorService service;

    private static final Integer THREE_MIN = 60 * 3;

    private static final Integer ONE_MIN = 60;

    private final Thread shutdownHook;

    private volatile Long workerId;

    public WorkerIdManager(RedisTemplate redisTemplate,
                           boolean openSequenceSetWorkerId,
                           String appKey) {
        this.redisTemplate = redisTemplate;
        this.openSequenceSetWorkerId = openSequenceSetWorkerId;
        this.appKey = appKey;
        this.service = Executors.newSingleThreadScheduledExecutor();
        // 钩子函数优雅关闭线程池
        this.shutdownHook = new Thread(() -> destroy());
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        // 续期任务
        renewalWorkerId();
    }

    /**
     * 获取 workerId
     * 先采用随机策略，失败再采用顺序策略，上游给出降级方案
     * @return
     */
    public synchronized Long registerAndGetWorkerId() {

        workerId = null;
        randomSetWorkerId();
        if (openSequenceSetWorkerId && workerId == null) {
            sequenceSetWorkerId();
        }
        return workerId;
    }

    /**
     * 随机策略
     * 取 0～1023 随机数
     * 终止条件：workerId 分配完成 或 或自旋次数大于 10
     */
    private void randomSetWorkerId() {

        int times = 0;
        while (workerId == null && times ++ < 10) {
            long random = ThreadLocalRandom.current().nextLong(0, 1024);
            workerId = register(random);
        }
    }

    /**
     * 顺序策略
     * 从0开始分配，失败后自增1
     * 终止条件：workerId 分配完成 或 workerId 分配完
     */
    private void sequenceSetWorkerId() {

        long preWorkId = 0;
        while (workerId == null) {
            workerId = register(preWorkId);
            // workerId 分配范围 [0,1024)
            if (++ preWorkId >= 1024) {
                log.error("无可用 workerId！！！");
                return;
            }
        }
    }

    /**
     * 到 Redis 中注册 workId
     * @return 如果失败返回 null，成功直接将 workerId 返回
     */
    private Long register(long workerId) {

        String key = buildSnowFlakeWorkerIdKey(appKey, workerId);
        try {
            if (redisTemplate.opsForValue().setIfAbsent(key, "1", THREE_MIN, TimeUnit.SECONDS)) {
                log.info("WorkerId={} register success", workerId);
                return workerId;
            }
        } catch (Exception e) {
            log.error("WorkerId register error", e);
        }
        return null;
    }

    /**
     * 续期 workerId
     */
    private void renewalWorkerId()  {

        service.scheduleWithFixedDelay(() -> {
            if (workerId != null) {
                String key = buildSnowFlakeWorkerIdKey(appKey, workerId);
                // 续期时再次set防止被Redis内存淘汰机制清理
                if (!redisTemplate.opsForValue().setIfAbsent(key, "1", THREE_MIN, TimeUnit.SECONDS)) {
                    redisTemplate.expire(key, THREE_MIN, TimeUnit.SECONDS);
                }
                log.info("workerId={} 续期成功", workerId);
            }
        }, 3L, ONE_MIN, TimeUnit.SECONDS);
    }

    private String buildSnowFlakeWorkerIdKey(String appKey, Long workerId) {

        return String.format("snow_flake_worker_%s_%s", appKey, workerId);
    }

    /**
     * 删除机器号
     * 一般发生时间回拨且集群规模较大的情况下，机器号进行切换后考虑删除上一个机器号
     * @param workerId
     */
    public void delWorkerId(Long workerId) {

        String key = buildSnowFlakeWorkerIdKey(appKey, workerId);
        redisTemplate.delete(key);
    }

    /**
     * 关闭线程池
     */
    public void destroy() {

        service.shutdown();
        log.info("续期线程池关闭");
        // 这里不删除上报机器号防止重启时时间回退造成唯一id发放重复
        // 但是需要保证重启时间小于 THREE_MIN - ONE_MIN = 2min
        // 如果后期项目重启时间过长可以适当调大有效期
    }

    public String getAppKey() {
        return appKey;
    }

    public Long getWorkerId() {
        return workerId;
    }
}
