package com.adong.fingermark.advice;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * @author ADong
 * @Description AOP 记录方法调用参数,耗时
 * @Date 2022-08-16 5:02 PM
 */

@Aspect
@Component
public class MethodLogger {

    private static final Logger logger = LoggerFactory.getLogger(MethodLogger.class);

    @Around("execution(* com.adong.fingermark.controller.*.*(..)))")
    public Object methodLogger(ProceedingJoinPoint jp) throws Throwable {

        Stopwatch stopwatch = Stopwatch.createStarted();
        Object retval = jp.proceed();

        List<String> param = Lists.newArrayList();
        if (jp.getArgs() != null) {
            for (Object obj : jp.getArgs()) {
                param.add(getData(obj));
            }
        }
        String log = "[METHOD_LOGGER]method_name=" +
                jp.getTarget().getClass().getName() + "." + jp.getSignature().getName() +
                ";process_time=" + stopwatch.elapsed(TimeUnit.MICROSECONDS) + "μs" +
                ";args=" + Arrays.toString(param.toArray()) +
                ";returnObj=" + getData(retval);
        logger.info(log);
        return retval;
    }

    private String getData(Object obj) {

        if (null == obj) {
            return "null";
        }
        if (obj instanceof HttpServletResponse) {
            return "null";
        }
        return JSON.toJSONString(obj);
    }
}
