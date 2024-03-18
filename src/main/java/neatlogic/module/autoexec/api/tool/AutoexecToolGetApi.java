/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.api.tool;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import neatlogic.framework.autoexec.constvalue.AutoexecFromType;
import neatlogic.framework.autoexec.constvalue.CombopOperationType;
import neatlogic.framework.autoexec.constvalue.ScriptAndToolOperate;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.exception.tool.AutoexecToolNotFoundEditTargetException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.dependency.dto.DependencyInfoVo;
import neatlogic.framework.dto.OperateVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.dependency.AutoexecTool2CombopPhaseOperationDependencyHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecToolGetApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Override
    public String getToken() {
        return "autoexec/tool/get";
    }

    @Override
    public String getName() {
        return "nmaat.autoexectoolgetapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "common.id"),
    })
    @Output({
            @Param(explode = AutoexecToolVo.class)
    })
    @Description(desc = "nmaat.autoexectoolgetapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AutoexecToolVo tool = autoexecToolMapper.getToolById(id);
        if (tool == null) {
            throw new AutoexecToolNotFoundEditTargetException(id);
        }
        List<Long> combopIdList = new ArrayList<>();
        List<DependencyInfoVo> dependencyInfoList = DependencyManager.getDependencyList(AutoexecTool2CombopPhaseOperationDependencyHandler.class, id);
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
            tool.setCombopList(combopList);
        }
//        tool.setCombopList(autoexecToolMapper.getReferenceListByToolId(id));
        List<OperateVo> operateList = new ArrayList<>();
        tool.setOperateList(operateList);
        OperateVo test = new OperateVo(ScriptAndToolOperate.TEST.getValue(), ScriptAndToolOperate.TEST.getText());
        OperateVo active = new OperateVo(ScriptAndToolOperate.ACTIVE.getValue(), ScriptAndToolOperate.ACTIVE.getText());
        OperateVo generateToCombop = new OperateVo(ScriptAndToolOperate.GENERATETOCOMBOP.getValue(), ScriptAndToolOperate.GENERATETOCOMBOP.getText());
        operateList.add(test);
        operateList.add(active);
        operateList.add(generateToCombop);
        if (AuthActionChecker.check(AUTOEXEC_COMBOP_ADD.class.getSimpleName())) {
            if (autoexecToolMapper.checkToolHasBeenGeneratedToCombop(id) > 0) {
                tool.setHasBeenGeneratedToCombop(1);
                generateToCombop.setDisabled(1);
                generateToCombop.setDisabledReason("已发布为组合工具");
            } else if (!Objects.equals(tool.getIsActive(), 1)) {
                generateToCombop.setDisabled(1);
                generateToCombop.setDisabledReason("当前工具未激活，无法发布为组合工具");
            }
        } else {
            generateToCombop.setDisabled(1);
            generateToCombop.setDisabledReason("无权限，请联系管理员");
        }
        if (!AuthActionChecker.check(AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
            test.setDisabled(1);
            test.setDisabledReason("无权限，请联系管理员");
        }
        if (!AuthActionChecker.check(AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName())) {
            active.setDisabled(1);
            active.setDisabledReason("无权限，请联系管理员");
        }
        tool.setType(CombopOperationType.TOOL.getValue());
        int count = DependencyManager.getDependencyCount(AutoexecFromType.TOOL, tool.getId());
        tool.setReferenceCount(count);
        return tool;
    }
}
