/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

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

package neatlogic.module.autoexec.api.combop;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.dao.mapper.ConfigMapper;
import neatlogic.framework.dto.ConfigVo;
import neatlogic.framework.autoexec.constvalue.AutoexecTenantConfig;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopVersionSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private ConfigMapper configMapper;

    @Resource
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getToken() {
        return "autoexec/combop/version/save";
    }

    @Override
    public String getName() {
        return "保存组合工具版本信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "主键id"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, isRequired = true, minLength = 1, maxLength = 70, desc = "显示名"),
            @Param(name = "status", type = ApiParamType.ENUM, rule = "draft,submitted", isRequired = true, desc = "状态"),
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具ID"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "配置信息")
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.LONG, desc = "主键id")
    })
    @Description(desc = "保存组合工具版本信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecCombopVersionVo autoexecCombopVersionVo = jsonObj.toJavaObject(AutoexecCombopVersionVo.class);

//        if (autoexecCombopVersionMapper.checkAutoexecCombopVersionNameIsRepeat(autoexecCombopVersionVo) != null) {
//            throw new AutoexecCombopVersionNameRepeatException(autoexecCombopVersionVo.getName());
//        }
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(autoexecCombopVersionVo.getCombopId());
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(autoexecCombopVersionVo.getCombopId());
        }
        if (Objects.equals(autoexecCombopVersionVo.getStatus(), ScriptVersionStatus.SUBMITTED.getValue())) {
            Long versionId = autoexecCombopVersionMapper.getAutoexecCombopMaxVersionIdByCombopIdAndStatus(autoexecCombopVersionVo.getCombopId(), autoexecCombopVersionVo.getStatus());
            if (versionId != null) {
                throw new AutoexecCombopHasSubmittedVersionException(autoexecCombopVo.getName());
            }
        }
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
        if (autoexecCombopVo.getEditable() == 0) {
            throw new PermissionDeniedException();
        }
        Long id = jsonObj.getLong("id");
        if (id == null) {
            if (!AuthActionChecker.checkByUserUuid(UserContext.get().getUserUuid(true), AUTOEXEC_COMBOP_ADD.class.getSimpleName())) {
                throw new PermissionDeniedException(AUTOEXEC_COMBOP_ADD.class);
            }
            AutoexecCombopVersionConfigVo config = autoexecCombopVersionVo.getConfig();
            String configStr = JSONObject.toJSONString(config);
            /** 保存前，校验组合工具是否配置正确，不正确不可以保存 **/
            autoexecCombopService.verifyAutoexecCombopVersionConfig(config, false);
            autoexecCombopVersionVo.setConfigStr(configStr);
            config = autoexecCombopVersionVo.getConfig();
            autoexecCombopService.resetIdAutoexecCombopVersionConfig(config);
            autoexecCombopService.setAutoexecCombopPhaseGroupId(config);
            autoexecCombopService.passwordParamEncrypt(config);
            autoexecCombopVersionVo.setConfigStr(null);
            Integer version = autoexecCombopVersionMapper.getAutoexecCombopMaxVersionByCombopId(autoexecCombopVersionVo.getCombopId());
            if (version == null) {
                version = 1;
            } else {
                version++;
            }
            autoexecCombopVersionVo.setVersion(version);
            autoexecCombopVersionVo.setIsActive(0);
            autoexecCombopVersionMapper.insertAutoexecCombopVersion(autoexecCombopVersionVo);
            autoexecCombopService.saveDependency(autoexecCombopVersionVo);
            int maxNum = Integer.parseInt(AutoexecTenantConfig.MAX_NUM_OF_COMBOP_VERSION.getValue());
            ConfigVo configVo = configMapper.getConfigByKey(AutoexecTenantConfig.MAX_NUM_OF_COMBOP_VERSION.getKey());
            if (configVo != null) {
                String value = configVo.getValue();
                if (StringUtils.isNotBlank(value)) {
                    try {
                        maxNum = Integer.parseInt(value);
                    } catch (NumberFormatException e) {

                    }
                }
            }
            List<AutoexecCombopVersionVo> versionList = autoexecCombopVersionMapper.getAutoexecCombopVersionListByCombopId(autoexecCombopVersionVo.getCombopId());
            if (versionList.size() > maxNum) {
                // 需要删除个数
                int deleteCount = versionList.size() - maxNum;
                // 根据版本id升序排序
                versionList.sort(Comparator.comparing(AutoexecCombopVersionVo::getId));
                // 遍历版本列表，删除最旧的非激活版本
                for (AutoexecCombopVersionVo versionVo : versionList) {
                    if (Objects.equals(versionVo.getIsActive(), 1)) {
                        continue;
                    }
                    autoexecCombopVersionMapper.deleteAutoexecCombopVersionById(versionVo.getId());
                    autoexecCombopService.deleteDependency(versionVo);
                    deleteCount--;
                    if (deleteCount == 0) {
                        break;
                    }
                }
            }
        } else {
            AutoexecCombopVersionVo oldAutoexecCombopVersionVo = autoexecCombopVersionMapper.getAutoexecCombopVersionById(id);
            if (oldAutoexecCombopVersionVo == null) {
                throw new AutoexecCombopVersionNotFoundException(id);
            }
            AutoexecCombopVersionConfigVo config = autoexecCombopVersionVo.getConfig();
            List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
            List<String> nameList = new ArrayList<>();
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                String name = autoexecCombopPhaseVo.getName();
                if (nameList.contains(name)) {
                    throw new AutoexecCombopPhaseNameRepeatException(name);
                }
                nameList.add(name);
            }

            autoexecCombopService.updateAutoexecCombopExecuteConfigProtocolAndProtocolPort(config);
            String configStr = JSONObject.toJSONString(config);
            /** 保存前，校验组合工具是否配置正确，不正确不可以保存 **/
            autoexecCombopService.verifyAutoexecCombopVersionConfig(config, false);
            autoexecCombopVersionVo.setConfigStr(configStr);
            autoexecCombopService.deleteDependency(oldAutoexecCombopVersionVo);
            config = autoexecCombopVersionVo.getConfig();
            autoexecCombopService.setAutoexecCombopPhaseGroupId(config);
            autoexecCombopService.passwordParamEncrypt(config);
//            autoexecCombopService.prepareAutoexecCombopVersionConfig(autoexecCombopVersionVo.getConfig(), false);
            autoexecCombopVersionVo.setConfigStr(null);
            autoexecCombopVersionMapper.updateAutoexecCombopVersionById(autoexecCombopVersionVo);
            autoexecCombopService.saveDependency(autoexecCombopVersionVo);
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("id", autoexecCombopVersionVo.getId());
        if (Objects.equals(autoexecCombopVersionVo.getStatus(), ScriptVersionStatus.SUBMITTED.getValue())) {
            resultObj.put("reviewable", autoexecCombopVo.getReviewable());
        }
        return resultObj;
    }

//    public IValid name() {
//        return jsonObj -> {
//            AutoexecCombopVersionVo autoexecCombopVersionVo = JSON.toJavaObject(jsonObj, AutoexecCombopVersionVo.class);
//            if (autoexecCombopVersionMapper.checkAutoexecCombopVersionNameIsRepeat(autoexecCombopVersionVo) != null) {
//                return new FieldValidResultVo(new AutoexecCombopVersionNameRepeatException(autoexecCombopVersionVo.getName()));
//            }
//            return new FieldValidResultVo();
//        };
//    }

}
