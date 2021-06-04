/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.job.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.text.ParseException;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/4/27 11:29
 **/
public interface AutoexecJobActionService {
    /**
     * 第一次执行/重跑/继续作业
     *
     * @param jobVo 作业
     */
    void fire(AutoexecJobVo jobVo);

    /**
     * 拼装给runner的param
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
    void abort(AutoexecJobVo jobVo);

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
     * 下载作业剧本节点执行情况
     *
     * @param jobPhaseNode 作业剧本节点
     * @param path         日志path
     */
    void logDownload(AutoexecJobPhaseNodeVo jobPhaseNode, String path);

    /**
     * 获取作业剧本节点执行记录
     *
     * @param paramObj 参数
     * @return 记录列表
     */
    List<AutoexecJobPhaseNodeAuditVo> getNodeAudit(JSONObject paramObj) throws ParseException;

    /**
     * 获取作业剧本节点操作状态
     *
     * @param paramObj 参数
     * @return 节点操作状态
     */
    List<AutoexecJobPhaseNodeOperationStatusVo> getNodeOperationStatus(JSONObject paramObj);

    /**
     * 获取作业console日志
     *
     * @param paramObj 参数
     * @return 日志内容
     */
    JSONObject tailConsoleLog(JSONObject paramObj);

    /**
     * 获取节点输出参数
     *
     * @param paramJson 参数
     * @return 输出参数
     */
    JSONArray getNodeOutputParam(JSONObject paramJson);

}
