/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.comboptemplate;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.CombopNodeSpecify;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopTemplateMapper;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import codedriver.framework.autoexec.dto.comboptemplate.AutoexecCombopTemplateVo;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.exception.AutoexecCombopExecuteNodeCannotBeEmptyException;
import codedriver.framework.autoexec.exception.AutoexecCombopExecuteParamCannotBeEmptyException;
import codedriver.framework.autoexec.exception.AutoexecCombopTemplateNotFoundException;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author linbq
 * @since 2021/4/22 18:09
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopTemplateNodeSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopTemplateMapper autoexecCombopTemplateMapper;
    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Override
    public String getToken() {
        return "autoexec/comboptemplate/node/save";
    }

    @Override
    public String getName() {
        return "保存组合工具模板执行目标信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopTemplateId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具模板主键id"),
            @Param(name = "protocolId", type = ApiParamType.LONG, desc = "连接协议id"),
            @Param(name = "executeUser", type = ApiParamType.STRING, desc = "执行用户"),
            @Param(name = "whenToSpecify", type = ApiParamType.ENUM, rule = "now,runtime,runtimeparam", isRequired = true, desc = "执行目标指定时机，现在指定/运行时再指定或指定运行参数"),
            @Param(name = "executeNodeConfig", type = ApiParamType.JSONOBJECT, desc = "执行目标信息")
    })
    @Description(desc = "保存组合工具模板执行目标信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopTemplateId = jsonObj.getLong("combopTemplateId");
        Long protocolId = jsonObj.getLong("protocolId");
        String executeUser = jsonObj.getString("executeUser");
        AccountProtocolVo protocolVo = resourceCenterMapper.getAccountProtocolVoByProtocolId(protocolId);
        if (protocolVo == null && protocolId != null) {
            throw new ResourceCenterAccountProtocolNotFoundException(protocolId);
        }
        AutoexecCombopTemplateVo autoexecCombopTemplateVo = autoexecCombopTemplateMapper.getAutoexecCombopById(combopTemplateId);
        if (autoexecCombopTemplateVo == null) {
            throw new AutoexecCombopTemplateNotFoundException(combopTemplateId);
        }
//        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
//        if (Objects.equals(autoexecCombopVo.getEditable(), 0)) {
//            throw new PermissionDeniedException();
//        }
        AutoexecCombopExecuteConfigVo executeConfig = new AutoexecCombopExecuteConfigVo();
        executeConfig.setProtocolId(protocolVo != null ? protocolVo.getId() : null);
        executeConfig.setProtocol(protocolVo != null ? protocolVo.getName() : null);
        executeConfig.setExecuteUser(executeUser);
        String whenToSpecify = jsonObj.getString("whenToSpecify");
        executeConfig.setWhenToSpecify(whenToSpecify);
        JSONObject executeNodeConfig = jsonObj.getJSONObject("executeNodeConfig");
        if (Objects.equals(whenToSpecify, CombopNodeSpecify.NOW.getValue())) {
            if (MapUtils.isEmpty(executeNodeConfig)) {
                throw new AutoexecCombopExecuteNodeCannotBeEmptyException();
            }
            AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = JSONObject.toJavaObject(executeNodeConfig, AutoexecCombopExecuteNodeConfigVo.class);
            List<AutoexecNodeVo> selectNodeList = executeNodeConfigVo.getSelectNodeList();
            List<AutoexecNodeVo> inputNodeList = executeNodeConfigVo.getInputNodeList();
            JSONObject filter = executeNodeConfigVo.getFilter();
            if (CollectionUtils.isEmpty(selectNodeList) && CollectionUtils.isEmpty(inputNodeList) && MapUtils.isEmpty(filter)) {
                throw new AutoexecCombopExecuteNodeCannotBeEmptyException();
            }
            executeConfig.setExecuteNodeConfig(executeNodeConfigVo);
        } else if (Objects.equals(whenToSpecify, CombopNodeSpecify.RUNTIMEPARAM.getValue())) {
            if (MapUtils.isEmpty(executeNodeConfig)) {
                throw new AutoexecCombopExecuteParamCannotBeEmptyException();
            }
            AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = JSONObject.toJavaObject(executeNodeConfig, AutoexecCombopExecuteNodeConfigVo.class);
            if (CollectionUtils.isEmpty(executeNodeConfigVo.getParamList())) { // 选择运行参数作为执行目标时，运行参数必填
                throw new AutoexecCombopExecuteParamCannotBeEmptyException();
            }
            executeConfig.setExecuteNodeConfig(executeNodeConfigVo);
        } else {
            AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo;
            if (MapUtils.isNotEmpty(executeNodeConfig)) {
                executeNodeConfigVo = JSONObject.toJavaObject(executeNodeConfig, AutoexecCombopExecuteNodeConfigVo.class);
            } else {
                executeNodeConfigVo = new AutoexecCombopExecuteNodeConfigVo();
            }
            executeConfig.setExecuteNodeConfig(executeNodeConfigVo);
        }
        AutoexecCombopConfigVo autoexecCombopConfigVo = autoexecCombopTemplateVo.getConfig();
        autoexecCombopConfigVo.setExecuteConfig(executeConfig);
        autoexecCombopTemplateVo.setFcu(UserContext.get().getUserUuid(true));
        autoexecCombopTemplateMapper.updateAutoexecCombopConfigById(autoexecCombopTemplateVo);
        return null;
    }
}