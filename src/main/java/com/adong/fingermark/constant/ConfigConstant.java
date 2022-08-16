package com.adong.fingermark.constant;

import com.adong.fingermark.core.SnowFlakeId;
import com.adong.fingermark.core.WorkerIdManager;

/**
 * @author ADong
 * @Description ConfigConstant
 * @Date 2022-08-15 5:59 PM
 */
public abstract class ConfigConstant {

    /** 机器号位 {@link SnowFlakeId#workerIdShift} **/
    public final static String WORKERID_SHIFT = "workerId.shift";

    /** 随机策略达到上限后是否启用顺序策略 {@link WorkerIdManager#sequenceSetWorkerId()} **/
    public final static String OPEN_SEQUENCE_SET_WORKERID = "open.sequence.set.workerId";

}
