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

package neatlogic.module.autoexec.api.script;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.script.*;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecScriptCopyApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/copy";
    }

    @Override
    public String getName() {
        return "复制脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "脚本ID"),
//            @Param(name = "uk", type = ApiParamType.REGEX, rule = "^[A-Za-z]+$", isRequired = true, xss = true, desc = "唯一标识"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, maxLength = 50, isRequired = true, xss = true, desc = "名称"),
            @Param(name = "execMode", type = ApiParamType.ENUM, rule = "runner,target,runner_target,sqlfile,native", desc = "执行方式"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "脚本分类ID", isRequired = true),
            @Param(name = "catalogId", type = ApiParamType.LONG, desc = "工具目录ID", isRequired = true),
            @Param(name = "riskId", type = ApiParamType.LONG, desc = "操作级别ID"),
            @Param(name = "isLib", type = ApiParamType.INTEGER, desc = "是否库文件（1：是，0：否，默认否）", isRequired = true),
            @Param(name = "customTemplateId", type = ApiParamType.LONG, desc = "自定义模版ID"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
    })
    @Output({
            @Param(type = ApiParamType.LONG, desc = "复制生成的脚本ID"),
    })
    @Description(desc = "复制脚本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AutoexecScriptVo targetScript = jsonObj.toJavaObject(AutoexecScriptVo.class);
        AutoexecScriptVo sourceScript = autoexecScriptMapper.getScriptBaseInfoById(targetScript.getId());
        if (sourceScript == null) {
            throw new AutoexecScriptNotFoundException(targetScript.getId());
        }
        targetScript.setId(null);
        targetScript.setFcu(UserContext.get().getUserUuid());
        autoexecScriptService.validateScriptBaseInfo(targetScript);
        autoexecScriptMapper.insertScript(targetScript);

        // 复制所有已通过版本
        List<AutoexecScriptVersionVo> sourceVersionList = autoexecScriptService
                .getScriptVersionDetailListByScriptId(new AutoexecScriptVersionVo(sourceScript.getId(), ScriptVersionStatus.PASSED.getValue()));
        if (CollectionUtils.isNotEmpty(sourceVersionList)) {
            List<AutoexecScriptVersionVo> targetVersionList = new ArrayList<>();
            List<AutoexecScriptVersionParamVo> paramList = new ArrayList<>();
            List<AutoexecScriptArgumentVo> argumentList = new ArrayList<>();
            List<AutoexecScriptLineVo> lineList = new ArrayList<>();
            for (AutoexecScriptVersionVo source : sourceVersionList) {
                AutoexecScriptVersionVo target = new AutoexecScriptVersionVo();
                BeanUtils.copyProperties(source, target);
                target.setId(null);
                target.setScriptId(targetScript.getId());
                targetVersionList.add(target);
                if (CollectionUtils.isNotEmpty(source.getParamList())) {
                    source.getParamList().forEach(o -> o.setScriptVersionId(target.getId()));
                    paramList.addAll(source.getParamList());
                }
                AutoexecScriptArgumentVo argument = source.getArgument();
                if (argument != null) {
                    argument.setScriptVersionId(target.getId());
                    argumentList.add(argument);
                }
                if (CollectionUtils.isNotEmpty(source.getLineList())) {
                    source.getLineList().forEach(o -> {
                        o.setId(null);
                        o.setScriptId(targetScript.getId());
                        o.setScriptVersionId(target.getId());
                    });
                    lineList.addAll(source.getLineList());
                }
                if (paramList.size() >= 100) {
                    autoexecScriptService.batchInsertScriptVersionParamList(paramList, 100);
                    paramList.clear();
                }
                if (lineList.size() >= 100) {
                    autoexecScriptService.batchInsertScriptLineList(lineList, 100);
                    lineList.clear();
                }
                if (CollectionUtils.isNotEmpty(source.getUseLib())) {
                    autoexecScriptMapper.insertScriptVersionUseLib(target.getId(), source.getUseLib());
                }
            }
            if (paramList.size() > 0) {
                autoexecScriptMapper.insertScriptVersionParamList(paramList);
            }
            if (argumentList.size() > 0) {
                autoexecScriptMapper.batchInsertVersionArgument(argumentList);
            }
            if (lineList.size() > 0) {
                autoexecScriptMapper.insertScriptLineList(lineList);
            }
            autoexecScriptMapper.batchInsertScriptVersion(targetVersionList);
        }

        return targetScript.getId();
    }


}
