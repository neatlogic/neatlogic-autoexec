/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.globallock;

import codedriver.framework.autoexec.constvalue.AutoexecOperType;
import codedriver.framework.dto.globallock.GlobalLockVo;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.globallock.GlobalLockManager;
import codedriver.framework.globallock.core.GlobalLockHandlerBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AutoexecJobGlobalLockHandler extends GlobalLockHandlerBase {
    @Override
    public String getHandler() {
        return AutoexecOperType.AUTOEXEC.getValue();
    }

    @Override
    public String getHandlerName() {
        return "自动化 fileLock";
    }

    @Override
    public boolean getIsCanLock(List<GlobalLockVo> globalLockVoList, GlobalLockVo globalLockVo) {
        String mode = globalLockVo.getHandlerParam().getString("mode");
        if(StringUtils.isBlank(mode)){
            throw new ParamIrregularException("mode");
        }
        for (GlobalLockVo globalLock : globalLockVoList){
            if(StringUtils.isNotBlank(mode) && !Objects.equals(globalLock.getHandlerParam().getString("mode"),mode)){
                globalLockVo.setWaitReason("your mode is '"+mode+"',already has '"+globalLock.getHandlerParam().getString("mode")+"' lock");
                return false;
            }
            if(StringUtils.isNotBlank(mode) && Objects.equals("write",mode ) && Objects.equals(globalLock.getHandlerParam().getString("mode"),mode)){
                globalLockVo.setWaitReason("your mode is '"+mode+"',already has '"+globalLock.getHandlerParam().getString("mode")+"' lock");
                return false;
            }
            mode = globalLock.getHandlerParam().getString("mode");
        }
        return true;
    }

    @Override
    public JSONObject getLock(JSONObject paramJson) {
        JSONObject jsonObject = new JSONObject();
        GlobalLockVo globalLockVo = new GlobalLockVo();
        globalLockVo.setHandler("autoexec");
        globalLockVo.setKey(paramJson.getString("jobId"));
        globalLockVo.setHandlerParamStr(paramJson.toJSONString());
        GlobalLockManager.getLock(globalLockVo);
        if (globalLockVo.getIsLock() == 1) {
            jsonObject.put("lockId", globalLockVo.getId());
            jsonObject.put("wait", 0);
        } else {
            jsonObject.put("wait", 1);
            jsonObject.put("message", globalLockVo.getWaitReason());
        }
        return jsonObject;
    }
}
