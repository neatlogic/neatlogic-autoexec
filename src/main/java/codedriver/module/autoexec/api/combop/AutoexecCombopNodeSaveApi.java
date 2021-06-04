/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_MODIFY;
import codedriver.framework.autoexec.constvalue.CombopNodeSpecify;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopExecuteConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopExecuteNodeConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.exception.AutoexecCombopExecuteNodeCannotBeEmptyException;
import codedriver.framework.autoexec.exception.AutoexecCombopExecuteUserCannotBeEmptyException;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
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
@AuthAction(action = AUTOEXEC_COMBOP_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopNodeSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

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
            @Param(name = "executeUser", type = ApiParamType.STRING, isRequired = true, desc = "执行用户"),
            @Param(name = "whenToSpecify", type = ApiParamType.ENUM, rule = "now,runtime", isRequired = true, desc = "执行目标指定时机，现在指定/运行时再指定"),
            @Param(name = "executeNodeConfig", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "执行目标信息")
    })
    @Description(desc = "保存组合工具执行目标信息")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopId = jsonObj.getLong("combopId");
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        AutoexecCombopExecuteConfigVo executeConfig = new AutoexecCombopExecuteConfigVo();

        String whenToSpecify = jsonObj.getString("whenToSpecify");
        executeConfig.setWhenToSpecify(whenToSpecify);
        if (Objects.equals(whenToSpecify, CombopNodeSpecify.NOW.getValue())) {
            String executeUser = jsonObj.getString("executeUser");
            if (StringUtils.isBlank(executeUser)) {
                throw new AutoexecCombopExecuteUserCannotBeEmptyException();
            }
            executeConfig.setExecuteUser(executeUser);
            JSONObject executeNodeConfig = jsonObj.getJSONObject("executeNodeConfig");
            if (MapUtils.isEmpty(executeNodeConfig)) {
                throw new AutoexecCombopExecuteNodeCannotBeEmptyException();
            }
            AutoexecCombopExecuteNodeConfigVo executeNodeConfigVo = JSONObject.toJavaObject(executeNodeConfig, AutoexecCombopExecuteNodeConfigVo.class);
            List<AutoexecNodeVo> selectNodeList = executeNodeConfigVo.getSelectNodeList();
            List<AutoexecNodeVo> inputNodeList = executeNodeConfigVo.getInputNodeList();
            List<String> paramList = executeNodeConfigVo.getParamList();
            List<Long> tagList = executeNodeConfigVo.getTagList();
            if (CollectionUtils.isEmpty(selectNodeList) && CollectionUtils.isEmpty(inputNodeList) && CollectionUtils.isEmpty(paramList) && CollectionUtils.isEmpty(tagList)) {
                throw new AutoexecCombopExecuteNodeCannotBeEmptyException();
            }
            executeConfig.setExecuteNodeConfig(executeNodeConfigVo);
        } else {
            executeConfig.setExecuteNodeConfig(new AutoexecCombopExecuteNodeConfigVo());
        }
        AutoexecCombopConfigVo autoexecCombopConfigVo = autoexecCombopVo.getConfig();
        autoexecCombopConfigVo.setExecuteConfig(executeConfig);
        autoexecCombopVo.setFcu(UserContext.get().getUserUuid(true));
        autoexecCombopMapper.updateAutoexecCombopConfigById(autoexecCombopVo);
        return null;
    }
}
