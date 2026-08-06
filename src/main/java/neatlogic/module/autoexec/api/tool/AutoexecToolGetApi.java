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

package neatlogic.module.autoexec.api.tool;

import neatlogic.framework.util.$;

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
import neatlogic.framework.autoexec.exception.tool.AutoexecToolNotFoundEditTargetException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.dto.OperateVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
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
//        List<Long> combopIdList = new ArrayList<>();
//        List<DependencyInfoVo> dependencyInfoList = DependencyManager.getDependencyList(AutoexecTool2CombopPhaseOperationDependencyHandler.class, id);
//        for (DependencyInfoVo dependencyInfoVo : dependencyInfoList) {
//            JSONObject config = dependencyInfoVo.getConfig();
//            if (MapUtils.isNotEmpty(config)) {
//                Long combopId = config.getLong("combopId");
//                if (combopId != null) {
//                    combopIdList.add(combopId);
//                }
//            }
//        }
//        if (CollectionUtils.isNotEmpty(combopIdList)) {
//            List<AutoexecCombopVo> combopList = autoexecCombopMapper.getAutoexecCombopByIdList(combopIdList);
//            tool.setCombopList(combopList);
//        }
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
                generateToCombop.setDisabledReason($.t("nmar.operate.publishedascombop"));
            } else if (!Objects.equals(tool.getIsActive(), 1)) {
                generateToCombop.setDisabled(1);
                generateToCombop.setDisabledReason($.t("nmar.operate.inactivetoolcannotpublish"));
            }
        } else {
            generateToCombop.setDisabled(1);
            generateToCombop.setDisabledReason($.t("nmar.operate.permissiondenied"));
        }
        if (!AuthActionChecker.check(AUTOEXEC_SCRIPT_MODIFY.class.getSimpleName())) {
            test.setDisabled(1);
            test.setDisabledReason($.t("nmar.operate.permissiondenied"));
        }
        if (!AuthActionChecker.check(AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName())) {
            active.setDisabled(1);
            active.setDisabledReason($.t("nmar.operate.permissiondenied"));
        }
        tool.setType(CombopOperationType.TOOL.getValue());
        int count = DependencyManager.getDependencyCount(AutoexecFromType.TOOL, tool.getId());
        tool.setReferenceCount(count);
        return tool;
    }
}
