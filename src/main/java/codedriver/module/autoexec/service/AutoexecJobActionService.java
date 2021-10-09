/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.job.*;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/4/27 11:29
 **/
public interface AutoexecJobActionService {
    /**
     * 作业执行动作权限校验
     *
     * @param jobVo 作业
     */
    void executeAuthCheck(AutoexecJobVo jobVo);

    /**
     * 第一次执行
     *
     * @param jobVo 作业
     */
    void fire(AutoexecJobVo jobVo);

    /**
     * 重跑
     *
     * @param type  重跑类型：   重置并重跑所有：refireResetAll；重跑所有：refireAll
     * @param jobVo 作业
     */
    void refire(AutoexecJobVo jobVo, String type);

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
     * 下载剧本节点执行日志
     *
     * @param paramJson 入参
     * @param response
     * @throws IOException
     */
    void downloadNodeLog(JSONObject paramJson, HttpServletResponse response) throws IOException;

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
     * @param jobVo 重置作业节点
     */
    void resetNode(AutoexecJobVo jobVo);

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
    AutoexecJobPhaseNodeVo getNodeOperationStatus(JSONObject paramObj);

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

    /**
     * 获取执行sql状态
     *
     * @param paramObj 参数
     * @return sql执行状态
     */
    AutoexecJobNodeSqlVo getNodeSqlStatus(JSONObject paramObj);

    /**
     * 获取node sql列表
     *
     * @param paramObj 参数
     * @return sql列表
     */
    List<AutoexecJobNodeSqlVo> getNodeSqlList(JSONObject paramObj);

    /**
     * 获取sql文件 内容
     *
     * @param paramObj 参数
     * @return sql文件 内容
     */
    String getNodeSqlContent(JSONObject paramObj);

    /**
     * 提交等待输入内容到 pipe
     * @param paramObj 参数
     */
    void submitWaitInput(JSONObject paramObj);

    /**
     * 校验根据组合工具创建的作业
     * @param isNeedAuth 是否需要鉴权
     */
    AutoexecJobVo validateCreateJobFromCombop(JSONObject param,boolean isNeedAuth);

    /**
     * 校验创建并激活作业
     * @param isNeedAuth 是否需要鉴权
     */
    void validateCreateJob(JSONObject param,boolean isNeedAuth);
}
