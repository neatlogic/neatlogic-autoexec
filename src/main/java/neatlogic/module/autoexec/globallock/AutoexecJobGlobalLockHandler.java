/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.globallock;

import neatlogic.framework.util.$;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobSourceType;
import neatlogic.framework.dto.globallock.GlobalLockVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.globallock.GlobalLockManager;
import neatlogic.framework.globallock.core.GlobalLockHandlerBase;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Resource;
import neatlogic.framework.autoexec.globallock.AutoexecJobGlobalLockService;

@Service
public class AutoexecJobGlobalLockHandler extends GlobalLockHandlerBase {
    @Resource
    private AutoexecJobGlobalLockService jobLockService;

    /** 在每次获锁事务内，通过作业行锁校验当前作业状态。 */
    @Override
    public void validateAcquisition(GlobalLockVo lock) { jobLockService.validate(lock); }

    /** 作业身份和归属规则由自动化业务层统一提供。 */
    @Override public void validateIdentity(GlobalLockVo existing, GlobalLockVo request) { jobLockService.validateIdentity(existing, request); }

    /** 兼容尚未回填归属字段的历史作业锁。 */
    @Override public boolean ownsLock(GlobalLockVo lock, String ownerId) { return jobLockService.ownsLock(lock, ownerId); }

    /** 提供作业及执行实例展示信息，不要求框架理解这些字段。 */
    @Override public JSONObject getLockIdentity(GlobalLockVo lock) { return jobLockService.identity(lock); }


    @Override
    public String getHandler() {
        return JobSourceType.AUTOEXEC.getValue();
    }

    @Override
    public String getHandlerName() {
        return $.t("nmar.globallock.name");
    }

    /** 检查读写冲突，等待原因同时标明申请锁和阻塞它的持有锁 ID。 */
    @Override
    public boolean getIsCanLock(List<GlobalLockVo> globalLockVoList, GlobalLockVo globalLockVo) {
        String lockMode = globalLockVo.getHandlerParam().getString("lockMode");
        if(StringUtils.isBlank(lockMode)){
            throw new ParamIrregularException("lockMode");
        }
        Optional<GlobalLockVo> lockedGlobalLockOptional = globalLockVoList.stream().filter(o-> Objects.equals(o.getIsLock(),1)).findFirst();
        if(lockedGlobalLockOptional.isPresent()) {
            GlobalLockVo lockedGlobalLock = lockedGlobalLockOptional.get();
            if (!Objects.equals(lockedGlobalLock.getHandlerParam().getString("lockMode"), lockMode)) {
                globalLockVo.setWaitReason("your mode is '" + lockMode + "' (lockId=" + globalLockVo.getId()
                        + "), already has '" + lockedGlobalLock.getHandlerParam().getString("lockMode")
                        + "' lock (lockId=" + lockedGlobalLock.getId() + ")");
                return false;
            }
            if (StringUtils.isNotBlank(lockMode) && Objects.equals("write", lockMode) && Objects.equals(lockedGlobalLock.getHandlerParam().getString("lockMode"), lockMode)) {
                globalLockVo.setWaitReason("your mode is '" + lockMode + "' (lockId=" + globalLockVo.getId()
                        + "), already has '" + lockedGlobalLock.getHandlerParam().getString("lockMode")
                        + "' lock (lockId=" + lockedGlobalLock.getId() + ")");
                return false;
            }
        }
        return true;
    }

    @Override
    public JSONObject getLock(JSONObject paramJson) {
        JSONObject jsonObject = new JSONObject();
        String jobId = paramJson.getString("jobId");
        if(StringUtils.isBlank(jobId)){
            throw new ParamIrregularException("jobId");
        }
        GlobalLockVo globalLockVo = new GlobalLockVo(JobSourceType.AUTOEXEC.getValue(),jobId,paramJson.toJSONString());
        GlobalLockManager.getLock(globalLockVo);
        if (globalLockVo.getIsLock() == 1) {
            jsonObject.put("wait", 0);
        } else {
            jsonObject.put("wait", 1);
            jsonObject.put("message", globalLockVo.getWaitReason());
        }
        jsonObject.put("lockId", globalLockVo.getId());
        return jsonObject;
    }

    @Override
    protected JSONObject myUnLock(Long lockId, JSONObject paramJson) {
        JSONObject jsonObject = new JSONObject();
        GlobalLockManager.unLock(lockId, paramJson);
        jsonObject.put("lockId",lockId);
        return jsonObject;
    }

    @Override
    public JSONObject retryLock(Long lockId, JSONObject paramJson) {
        JSONObject jsonObject = new JSONObject();
        if(lockId == null){
            throw new ParamIrregularException("lockId");
        }
        //预防如果不存在，需重新insert lock
        String jobId = paramJson.getString("jobId");
        GlobalLockVo globalLockVo = new GlobalLockVo(lockId, JobSourceType.AUTOEXEC.getValue(),jobId,paramJson.toJSONString());
        globalLockVo = GlobalLockManager.retryLock(globalLockVo);
        if (globalLockVo.getIsLock() == 1) {
            jsonObject.put("lockId", globalLockVo.getId());
        } else {
           throw new ApiRuntimeException(globalLockVo.getWaitReason());
        }
        return jsonObject;
    }
}
