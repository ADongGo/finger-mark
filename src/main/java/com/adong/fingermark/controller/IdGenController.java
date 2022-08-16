package com.adong.fingermark.controller;

import com.adong.fingermark.common.Result;
import com.adong.fingermark.manager.IdGenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ADong
 * @Description IdGenController
 * @Date 2022-08-15 8:02 PM
 */

@RestController
@RequestMapping("/idGen")
public class IdGenController {

    private static final Logger log = LoggerFactory.getLogger(IdGenController.class);

    @Autowired
    private IdGenManager idGenManager;

    @GetMapping("/getIdByAppKey/{appKey}")
    public Result getIdByAppKey(@PathVariable("appKey") String appKey) {
        try {
            return Result.success(idGenManager.getId(appKey));
        } catch (Exception e) {
            log.error("getIdByAppKey error", e);
            return Result.error();
        }
    }

    @GetMapping("/getId")
    public Result getId() {
        try {
            return Result.success(idGenManager.getId());
        } catch (Exception e) {
            log.error("getId error", e);
            return Result.error();
        }
    }
}
