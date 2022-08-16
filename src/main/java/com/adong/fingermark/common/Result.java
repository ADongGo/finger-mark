package com.adong.fingermark.common;

import com.adong.fingermark.constant.Status;

public class Result {

    private static final long ERROR_ID = -1;

    private static final String ERROR_MSG = "system error!";

    private long id;
    private Status status;
    private String msg;

    public Result() {

    }
    public Result(long id, Status status, String msg) {
        this.id = id;
        this.status = status;
        this.msg = msg;
    }

    public static Result success(long id) {
        return new Result(id, Status.SUCCESS, null);
    }

    public static Result error() {
        return new Result(ERROR_ID, Status.ERROR, ERROR_MSG);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
