/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.job.AutoexecJobLogVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import com.alibaba.fastjson.JSONObject;

/**
 * @author lvzk
 * @since 2021/4/27 11:29
 **/
public interface AutoexecJobActionService {
    /**
     * 第一次执行/重跑/继续作业
     *
     * @param jobVo 作业
     * @param type  重跑redo，第一次跑 first, 继续跑 goon
     */
    void fire(AutoexecJobVo jobVo, String type);

    /**
     * 拼装给proxy的param
     *
     * @param paramJson 返回param值
     * @param jobVo     作业
     */
    void getFireParamJson(JSONObject paramJson, AutoexecJobVo jobVo);

    /**
     * 实时获取剧本节点执行日志
     *
     * @param paramJson 入参
     * @return 执行日志
     */
    JSONObject tailNodeLog(JSONObject paramJson);

    /**
     * 暂停作业
     *
     * @param jobVo 作业
     */
    void pause(AutoexecJobVo jobVo);

    /**
     * 中止作业
     *
     * @param jobVo 作业
     */
    void stop(AutoexecJobVo jobVo);

    /**
     * 重置作业节点
     *
     * @param jobPhaseNode 重置作业节点
     */
    void reset(AutoexecJobPhaseNodeVo jobPhaseNode);

    /**
     * 忽略作业节点
     *
     * @param jobPhase 作业剧本
     */
    public void ignore(AutoexecJobPhaseVo jobPhase);

    /**
     * 实时获取作业剧本节点执行情况
     *
     * @param jobPhaseNode 作业剧本节点
     * @param position     日志位置
     * @param path         日志path
     * @return 日志内容
     */
    AutoexecJobLogVo logTail(AutoexecJobPhaseNodeVo jobPhaseNode, Integer position, String path);

    /**
     * 下载作业剧本节点执行情况
     *
     * @param jobPhaseNode 作业剧本节点
     * @param path         日志path
     */
    void logDownload(AutoexecJobPhaseNodeVo jobPhaseNode, String path);
}
