/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobInvokeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import com.alibaba.fastjson.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * @since 2021/4/12 18:44
 **/
public interface AutoexecJobService {
    /**
     * 通过combopVo保存作业配置
     *
     * @param combopVo    组合工具vo
     * @param threadCount 并发线程数
     * @return jobId
     */
    AutoexecJobVo saveAutoexecCombopJob(AutoexecCombopVo combopVo, AutoexecJobInvokeVo invokeVo, Integer threadCount, JSONObject paramJson) throws Exception;

    /**
     * 通过combopVo保存作业配置
     *
     * @param combopVo      组合工具vo
     * @param threadCount   并发线程数
     * @param planStartTime 计划时间
     * @param triggerType   触发方式
     * @return
     * @throws Exception
     */
    AutoexecJobVo saveAutoexecCombopJob(AutoexecCombopVo combopVo, AutoexecJobInvokeVo invokeVo, Integer threadCount, JSONObject paramJson, Date planStartTime, String triggerType) throws Exception;

    /**
     * sort 为null 则补充job全部信息 ，否则返回当前sort的所有剧本
     *
     * @param jobVo 作业概要
     */
    void getAutoexecJobDetail(AutoexecJobVo jobVo);

    /**
     * 判断是否所有并行剧本都跑完
     *
     * @param jobId 作业id
     * @param sort  作业剧本顺序
     * @return true:都跑完 false:存在没跑完的
     */
    boolean checkIsAllActivePhaseIsCompleted(Long jobId, Integer sort);

    /**
     * 刷新激活剧本的所有节点信息
     * 1、找到所有满足条件的执行节点update 如果update 返回值为0 则 insert
     * 2、删除所有状态为"pending"（即没跑过的历史节点）的node 以及对应的runner
     * 3、update 剩下所有job_node（即跑过的历史节点） lcd小于 phase lcd 的作业节点 标示 "is_delete" = 1
     * 4、删除该阶段所有不是最近更新的phase runner
     *
     * @param jobId          作业id
     * @param jobPhaseVoList 需要刷新节点的phase
     * @param executeConfig  执行时的参数（执行目标，用户，协议）
     */
    void refreshJobPhaseNodeList(Long jobId, List<AutoexecJobPhaseVo> jobPhaseVoList, JSONObject executeConfig);

    /**
     * 刷新激活剧本的所有节点信息
     * 1、找到所有满足条件的执行节点update 如果update 返回值为0 则 insert
     * 2、删除所有状态为"pending"（即没跑过的历史节点）的node 以及对应的runner
     * 3、update 剩下所有job_node（即跑过的历史节点） lcd小于 phase lcd 的作业节点 标示 "is_delete" = 1
     * 4、删除该阶段所有不是最近更新的phase runner
     *
     * @param jobId          作业id
     * @param jobPhaseVoList 需要刷新节点的phase
     */
    void refreshJobPhaseNodeList(Long jobId, List<AutoexecJobPhaseVo> jobPhaseVoList);

    /**
     * 刷新作业所有阶段节点信息
     * 遍历phaseList刷新节点
     *
     * @param jobId         作业id
     * @param executeConfig 执行时的参数（执行目标，用户，协议）
     */
    void refreshJobNodeList(Long jobId, JSONObject executeConfig);

    /**
     * 刷新作业所有阶段节点信息
     * 遍历phaseList刷新节点
     *
     * @param jobId 作业id
     */
    void refreshJobNodeList(Long jobId);

    /**
     * 刷新作业运行参数
     *
     * @param jobId     作业id
     * @param paramJson 运行参数json
     */
    void refreshJobParam(Long jobId, JSONObject paramJson);

    /**
     * 是否需要定时刷新
     *
     * @param paramObj 结果json
     * @param JobVo    作业
     * @param status   上一次状态，得确保上两次状态的查询都是"已完成"或"已成功"，前端才停止刷新
     */
    void setIsRefresh(List<AutoexecJobPhaseVo> jobPhaseList, JSONObject paramObj, AutoexecJobVo JobVo, String status);

    /**
     * 删除作业
     *
     * @param jobId 作业id
     */
    void deleteJob(Long jobId);

}
