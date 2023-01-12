/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_REVIEW;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVersionConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dto.FieldValidResultVo;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.IValid;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.RegexUtils;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
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

        if (autoexecCombopVersionMapper.checkAutoexecCombopVersionNameIsRepeat(autoexecCombopVersionVo) != null) {
            throw new AutoexecCombopVersionNameRepeatException(autoexecCombopVersionVo.getName());
        }
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(autoexecCombopVersionVo.getCombopId());
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(autoexecCombopVersionVo.getCombopId());
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
            autoexecCombopService.resetIdAutoexecCombopVersionConfig(config);
            autoexecCombopService.setAutoexecCombopPhaseGroupId(config);
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
            autoexecCombopService.setAutoexecCombopPhaseGroupId(autoexecCombopVersionVo.getConfig());
//            autoexecCombopService.prepareAutoexecCombopVersionConfig(autoexecCombopVersionVo.getConfig(), false);
            autoexecCombopVersionVo.setConfigStr(null);
            autoexecCombopVersionMapper.updateAutoexecCombopVersionById(autoexecCombopVersionVo);
            autoexecCombopService.saveDependency(autoexecCombopVersionVo);
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("id", autoexecCombopVersionVo.getId());
        if (Objects.equals(autoexecCombopVersionVo.getStatus(), ScriptVersionStatus.SUBMITTED.getValue())) {
            resultObj.put("reviewable", AuthActionChecker.check(AUTOEXEC_COMBOP_REVIEW.class) ? 1 : 0);
        }
        return resultObj;
    }

    public IValid name() {
        return jsonObj -> {
            AutoexecCombopVersionVo autoexecCombopVersionVo = JSON.toJavaObject(jsonObj, AutoexecCombopVersionVo.class);
            if (autoexecCombopVersionMapper.checkAutoexecCombopVersionNameIsRepeat(autoexecCombopVersionVo) != null) {
                return new FieldValidResultVo(new AutoexecCombopVersionNameRepeatException(autoexecCombopVersionVo.getName()));
            }
            return new FieldValidResultVo();
        };
    }

}
