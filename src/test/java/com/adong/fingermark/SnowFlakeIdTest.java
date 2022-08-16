package com.adong.fingermark;

import com.adong.fingermark.manager.IdGenManager;
import com.google.common.base.Stopwatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author ADong
 * @Description SnowFlakeIdTest
 * @Date 2022-08-16 2:25 PM
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class SnowFlakeIdTest {

    private static final Logger log = LoggerFactory.getLogger(SnowFlakeIdTest.class);

    @Autowired
    private IdGenManager idGenManager;

    private int threadSize = 100;

    private final ExecutorService task =  new ThreadPoolExecutor(threadSize,
            threadSize,
            0L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadPoolExecutor.CallerRunsPolicy());

    @Test
    public void testUUID() throws Exception {

        List<String> list = Collections.synchronizedList(new ArrayList<>());
        Stopwatch stopwatch = Stopwatch.createStarted();
        CountDownLatch count = new CountDownLatch(threadSize);
        // 100 个线程，每个线程执行 10000 次
        for (int m = 0; m < threadSize; m++) {
            task.execute(() -> {
                for (int i = 0; i < 20000; i++) {
                    try {
                        list.add(UUID.randomUUID().toString());
                    } catch (Exception e) {
                        log.error("get id error", e);
                    }
                }
                count.countDown();
            });
        }
        count.await();
        log.info("set size={}, 耗时={}", new HashSet<>(list).size(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        task.shutdown();
    }

    @Test
    public void testSnowFlakeId() throws Exception {

        List<Long> list = Collections.synchronizedList(new ArrayList<>());
        Stopwatch stopwatch = Stopwatch.createStarted();
        CountDownLatch count = new CountDownLatch(threadSize);
        // 100 个线程，每个线程执行 10000 次
        for (int m = 0; m < threadSize; m++) {
            task.execute(() -> {
                for (int i = 0; i < 10000; i++) {
                    try {
                        list.add(idGenManager.getId());
                    } catch (Exception e) {
                        log.error("get id error", e);
                    }
                }
                count.countDown();
            });
        }
        count.await();
        log.info("set size={}, 耗时={}", new HashSet<>(list).size(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        task.shutdown();
    }

}
