/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.autoexec.api.combop;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.constvalue.CombopNodeSpecify;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.dto.node.AutoexecNodeVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopExecuteNodeCannotBeEmptyException;
import neatlogic.framework.autoexec.exception.AutoexecCombopExecuteParamCannotBeEmptyException;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountProtocolVo;
import neatlogic.framework.cmdb.exception.resourcecenter.ResourceCenterAccountProtocolNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecCombopService;
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
//@Service
@Deprecated
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopNodeSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getToken() {
        return "autoexec/combop/node/save";
    }

    @Override
    public String getName() {
        return "保存组合工具执行目标信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具主键id"),
            @Param(name = "protocolId", type = ApiParamType.LONG, desc = "连接协议id"),
            @Param(name = "executeUser", type = ApiParamType.STRING, desc = "执行用户"),
            @Param(name = "roundCount", type = ApiParamType.STRING, desc = "分批数量"),
            @Param(name = "whenToSpecify", type = ApiParamType.ENUM, rule = "now,runtime,runtimeparam", isRequired = true, desc = "执行目标指定时机，现在指定/运行时再指定或指定运行参数"),
            @Param(name = "executeNodeConfig", type = ApiParamType.JSONOBJECT, desc = "执行目标信息")
    })
    @Description(desc = "保存组合工具执行目标信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopId = jsonObj.getLong("combopId");
        Long protocolId = jsonObj.getLong("protocolId");
        String executeUser = jsonObj.getString("executeUser");
        Integer roundCount = jsonObj.getInteger("roundCount");
        IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
        AccountProtocolVo protocolVo = null;
        if (protocolId != null) {
            protocolVo = resourceAccountCrossoverMapper.getAccountProtocolVoByProtocolId(protocolId);
            if (protocolVo == null) {
                throw new ResourceCenterAccountProtocolNotFoundException(protocolId);
            }
        }
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
        if (Objects.equals(autoexecCombopVo.getEditable(), 0)) {
            throw new PermissionDeniedException();
        }
        AutoexecCombopExecuteConfigVo executeConfig = new AutoexecCombopExecuteConfigVo();
        if (protocolVo != null) {
            executeConfig.setProtocolId(protocolVo.getId());
            executeConfig.setProtocol(protocolVo.getName());
            executeConfig.setProtocolPort(protocolVo.getPort());
        }
        executeConfig.setExecuteUser(executeUser);
        executeConfig.setRoundCount(roundCount);
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
        AutoexecCombopConfigVo autoexecCombopConfigVo = autoexecCombopVo.getConfig();
        autoexecCombopConfigVo.setExecuteConfig(executeConfig);
        autoexecCombopVo.setFcu(UserContext.get().getUserUuid(true));
        autoexecCombopVo.setConfigStr(null);
        autoexecCombopMapper.updateAutoexecCombopConfigById(autoexecCombopVo);
        return null;
    }
}
