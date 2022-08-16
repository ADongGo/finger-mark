package com.adong.fingermark.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author ADong
 * @Description 参考雪花算法实现分布式id
 * 1位正负位 + 41位时间戳 + 10位机器号 + 12位随机序列
 * 通过 Redis {@link WorkerIdManager#renewalWorkerId()}
 * 续期逻辑保证重启时不会注册上一个机器号来防止重启时的时间回拨问题
 * @Date 2022-08-15 4:24 PM
 */
public class SnowFlakeId implements IdGen {

    private static final Logger log = LoggerFactory.getLogger(SnowFlakeId.class);

    // 时间 41位
    private long lastTime = timeGen();

    // 机房机器ID 10位
    private long workerId = 0;
    private long workerIdShift = 10;

    // 随机数 12位
    private long random = 0;
    private long randomShift = 12;
    // 随机数的最大值
    private long maxRandom = (long) Math.pow(2, randomShift);

    private WorkerIdManager workerIdManager;

    public SnowFlakeId() {}

    public SnowFlakeId(long workerId, long workerIdShift, WorkerIdManager workerIdManager){
        if (workerIdShift < 0 || workerIdShift  > 22) {
            throw new IllegalArgumentException("workerIdShift set error!");
        }
        this.workerId = workerId;
        this.workerIdShift = workerIdShift;
        this.randomShift = 22 - workerIdShift;
        this.maxRandom = (long) Math.pow(2, randomShift);
        this.workerIdManager = workerIdManager;
        log.info("SnowFlakeId init success, workerId={}", workerId);
    }

    /**
     * 获取 id
     * @return
     */
    private long getId() {
        return lastTime << (workerIdShift + randomShift) |
                workerId << randomShift |
                random;
    }

    /**
     * 降级方案获取 id
     * @param badRandom
     * @return
     */
    private long getBadId(long badRandom) {
        return lastTime << (workerIdShift + randomShift) |
                badRandom;
    }

    /**
     * 生成新 Id
     * @return
     */
    @Override
    public synchronized long nextId() {

        long now = timeGen();

        //如果当前时间大于上次时间，直接返回
        if (now > lastTime) {
            lastTime = now;
            random = getRandom(100);
            return getId();
        }
        // 如果当前时间等于上次时间且random小于最大值随机序列
        if (now == lastTime && ++ random < maxRandom) {
            return getId();
        }
        // 判断如果回拨时间小于5ms或大于等于最大序列值就进行等待，否则进行降级方案
        long offset = lastTime - now == 0 ? 1 : lastTime - now;
        if (offset <= 5) {
            try {
                // 等待两倍offset
                wait(offset << 1);
            } catch (InterruptedException e) {
                log.error("nextId wait interrupted");
                return getBadId(getBadMaxRandom());
            }
        } else {
            // 发生时间回拨切换机器号，失败兜底
            if (!changeWorkerId()) {
                return getBadId(getBadMaxRandom());
            }
        }
        now = timeGen();
        // 再次判断，如果时间还不符合进行降级方案
        if (now < lastTime) {
            return getBadId(getBadMaxRandom());
        } else {
            now = tilNextMillis(lastTime);
        }
        lastTime = now;
        random = getRandom(100);
        return getId();
    }

    private long tilNextMillis(long lastTime) {
        long now = timeGen();
        while (now <= lastTime) {
            now = timeGen();
        }
        return now;
    }

    /**
     * 如果发生时间回拨，重新注册机器号
     * 如果集群规模较大，机器号进行切换后考虑删除上一个机器号 {@link WorkerIdManager#delWorkerId(Long)}
     * @return 是否切换成功
     */
    private boolean changeWorkerId() {

        if (!Objects.isNull(workerIdManager)) {
            Long changeWorkerId = workerIdManager.registerAndGetWorkerId();
            if (changeWorkerId != null) {
                workerId = changeWorkerId;
                lastTime = timeGen();
                // TODO 考虑是否清空上个机器号
                return true;
            }
        }
        return false;
    }

    /**
     * 降级方案获取最大随机序列位数
     * @return
     */
    private long getBadMaxRandom() {

        return getRandom((long) Math.pow(2, randomShift + workerIdShift));
    }

    /**
     * 根据最大限制获取随机序列
     * @param bound
     * @return
     */
    private long getRandom(long bound) {

        return ThreadLocalRandom.current().nextLong(bound);
    }

    /**
     * 生成当前时间
     * @return
     */
    private long timeGen() {

        return System.currentTimeMillis();
    }

    @Override
    public void destory() {
        if (!Objects.isNull(workerIdManager)) {
            workerIdManager.destroy();
        }
    }
}
