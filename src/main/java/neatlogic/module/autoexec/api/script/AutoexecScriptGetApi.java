/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.autoexec.api.script;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import neatlogic.framework.autoexec.constvalue.*;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptAuditVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVo;
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dependency.AutoexecScript2CombopPhaseOperationDependencyHandler;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private FileMapper fileMapper;

    @Override
    public String getToken() {
        return "autoexec/script/get";
    }

    @Override
    public String getName() {
        return "查看脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "脚本ID，表示不指定版本查看，两个参数二选一"),
            @Param(name = "versionId", type = ApiParamType.LONG, desc = "脚本版本ID，表示指定版本查看"),
            @Param(name = "status", type = ApiParamType.ENUM, rule = "draft,passed,rejected", desc = "状态(传id而非versionId时，表示从列表查看脚本，此时必须传status参数)"),
    })
    @Output({
            @Param(name = "script", explode = AutoexecScriptVo.class, desc = "脚本内容"),
    })
    @Description(desc = "查看脚本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        AutoexecScriptVo script = null;
        AutoexecScriptVersionVo version = null;
        Long id = jsonObj.getLong("id");
        Long versionId = jsonObj.getLong("versionId");
        String status = jsonObj.getString("status");
        if (id == null && versionId == null) {
            throw new ParamNotExistsException("id", "versionId");
        }
        if (id != null) { // 不指定版本
            if (StringUtils.isBlank(status)) {
                throw new ParamNotExistsException("status");
            }
            if (autoexecScriptMapper.checkScriptIsExistsById(id) == 0) {
                throw new AutoexecScriptNotFoundException(id);
            }
            /**
             * 如果是从已通过列表进入详情页，则取当前激活版本
             * 如果是从草稿或已驳回列表进入，则取最近修改的草稿或已驳回版本
             * 从待审批列表进入，调compare接口
             */
            if (Objects.equals(ScriptVersionStatus.PASSED.getValue(), status)) {
                AutoexecScriptVersionVo activeVersion = autoexecScriptMapper.getActiveVersionByScriptId(id);
                if (activeVersion != null) {
                    version = activeVersion;
                } else {
                    throw new AutoexecScriptVersionHasNoActivedException();
                }
            } else if (Objects.equals(ScriptVersionStatus.DRAFT.getValue(), status)) {
                AutoexecScriptVersionVo recentlyDraftVersion = autoexecScriptMapper.getRecentlyVersionByScriptIdAndStatus(id, ScriptVersionStatus.DRAFT.getValue());
                if (recentlyDraftVersion != null) {
                    version = recentlyDraftVersion;
                } else {
                    throw new AutoexecScriptHasNoDraftVersionException();
                }
            } else if (Objects.equals(ScriptVersionStatus.REJECTED.getValue(), status)) {
                AutoexecScriptVersionVo recentlyRejectedVersion = autoexecScriptMapper.getRecentlyVersionByScriptIdAndStatus(id, ScriptVersionStatus.REJECTED.getValue());
                if (recentlyRejectedVersion != null) {
                    version = recentlyRejectedVersion;
                } else {
                    throw new AutoexecScriptHasNoRejectedVersionException();
                }
            }
        } else if (versionId != null) { // 指定查看某个版本
            AutoexecScriptVersionVo currentVersion = autoexecScriptMapper.getVersionByVersionId(versionId);
            if (currentVersion == null) {
                throw new AutoexecScriptVersionNotFoundException(versionId);
            }
            // 已通过版本不显示标题
            if (Objects.equals(currentVersion.getStatus(), ScriptVersionStatus.PASSED.getValue())) {
                currentVersion.setTitle(null);
            }
            version = currentVersion;
            id = version.getScriptId();
        }
        script = autoexecScriptMapper.getScriptBaseInfoById(id);
        if (script == null) {
            throw new AutoexecScriptNotFoundException(id);
        }
        script.setVersionVo(version);
        AutoexecScriptVersionVo currentVersion = autoexecScriptMapper.getActiveVersionByScriptId(id);
        script.setCurrentVersionVo(currentVersion);
        List<AutoexecScriptVersionParamVo> paramList = autoexecScriptMapper.getParamListByVersionId(version.getId());
        version.setParamList(paramList);
        version.setArgument(autoexecScriptMapper.getArgumentByVersionId(version.getId()));
        if (!StringUtils.equals(version.getParser(), ScriptParser.PACKAGE.getValue())) {
            version.setLineList(autoexecScriptMapper.getLineListByVersionId(version.getId()));
        } else if (version.getPackageFileId() != null) {
            version.setPackageFile(fileMapper.getFileById(version.getPackageFileId()));
        }
        List<Long> combopIdList = new ArrayList<>();
        List<DependencyInfoVo> dependencyInfoList = DependencyManager.getDependencyList(AutoexecScript2CombopPhaseOperationDependencyHandler.class, id);
        for (DependencyInfoVo dependencyInfoVo : dependencyInfoList) {
            JSONObject config = dependencyInfoVo.getConfig();
            if (MapUtils.isNotEmpty(config)) {
                Long combopId = config.getLong("combopId");
                if (combopId != null) {
                    combopIdList.add(combopId);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(combopIdList)) {
            List<AutoexecCombopVo> combopList = autoexecCombopMapper.getAutoexecCombopByIdList(combopIdList);
            script.setCombopList(combopList);
            autoexecCombopService.setOperableButtonList(combopList);
        }
//        List<AutoexecCombopVo> combopList = autoexecScriptMapper.getReferenceListByScriptId(id);
//        script.setCombopList(combopList);
//        autoexecCombopService.setOperableButtonList(combopList);
        if (StringUtils.isNotBlank(version.getReviewer())) {
            version.setReviewerVo(userMapper.getUserBaseInfoByUuid(version.getReviewer()));
        }
        // 如果是已驳回状态，查询驳回原因
        if (ScriptVersionStatus.REJECTED.getValue().equals(version.getStatus())) {
            AutoexecScriptAuditVo audit = autoexecScriptMapper.getScriptAuditByScriptVersionIdAndOperate(version.getId(), ScriptAction.REJECT.getValue());
            if (audit != null) {
                String detail = autoexecScriptMapper.getScriptAuditDetailByHash(audit.getContentHash());
                if (StringUtils.isNotBlank(detail)) {
                    version.setRejectReason((String) JSONPath.read(detail, "content"));
                }
            }
        }
        // 获取操作按钮
        version.setOperateList(autoexecScriptService.getOperateListForScriptVersion(version));
        //获取依赖工具
        version.setUseLib(autoexecScriptMapper.getLibScriptIdListByVersionId(version.getId()));
        script.setType(CombopOperationType.SCRIPT.getValue());
        int count = DependencyManager.getDependencyCount(AutoexecFromType.SCRIPT, script.getId());
        script.setReferenceCount(count);
        result.put("script", script);
        return result;
    }


}
