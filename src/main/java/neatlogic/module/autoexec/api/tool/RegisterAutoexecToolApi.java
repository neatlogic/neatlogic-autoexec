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

package neatlogic.module.autoexec.api.tool;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_MODIFY;
import neatlogic.framework.autoexec.constvalue.*;
import neatlogic.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecToolMapper;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.AutoexecToolVo;
import neatlogic.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.autoexec.script.paramtype.ScriptParamTypeFactory;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.exception.type.ParamTypeNotFoundException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.I18nUtils;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import neatlogic.module.autoexec.service.AutoexecService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class RegisterAutoexecToolApi extends PrivateApiComponentBase {

    final Pattern defualtValuePattern = Pattern.compile("^\\s*\\$\\{\\s*global\\s*:\\s*(.*?)\\s*\\}\\s*$");

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Resource
    private AutoexecGlobalParamMapper autoexecGlobalParamMapper;

    @Resource
    private AutoexecService autoexecService;

    @Override
    public String getToken() {
        return "autoexec/tool/register";
    }

    @Override
    public String getName() {
        return "nmaat.registerautoexectoolapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "opName", type = ApiParamType.REGEX, rule = RegexUtils.NAME_WITH_SLASH, maxLength = 50, isRequired = true, desc = "common.name"),
            @Param(name = "opType", type = ApiParamType.ENUM, rule = "runner,target,runner_target,sqlfile,native", isRequired = true, desc = "term.autoexec.execmode"),
            @Param(name = "typeName", type = ApiParamType.REGEX, rule = RegexUtils.NAME, maxLength = 50, isRequired = true, desc = "common.typename"),
            @Param(name = "riskName", type = ApiParamType.REGEX, rule = RegexUtils.NAME, maxLength = 50, isRequired = true, desc = "term.autoexec.riskname"),
            @Param(name = "interpreter", type = ApiParamType.ENUM, rule = "python,ruby,vbscript,perl,powershell,cmd,bash,ksh,csh,sh,javascript", isRequired = true, desc = "term.autoexec.scriptparser"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "common.description"),
            @Param(name = "option", type = ApiParamType.JSONARRAY, desc = "term.autoexec.inputparamlist",
                    help = "当控件类型为[select,multiselect,radio,checkbox]时，需要在dataSource字段填写数据源，格式如下：[{\"text\":\"否\",\"value\":\"0\"},{\"text\":\"是\",\"value\":\"1\"}]，defaultValue字段填写数据源中的value值"),
            @Param(name = "argument", type = ApiParamType.JSONOBJECT, desc = "term.autoexec.freeparam", help = "{\n" +
                    "        \"name\":\"日志路径\",\n" +
                    "        \"help\":\"日志路径，支持通配符和反引号\",\n" +
                    "        \"type\":\"input\",\n" +
                    "        \"isConst\":\"false\",\n" +
                    "        \"defaultValue\":\"\",\n" +
                    "        \"required\":\"true\",\n" +
                    "        \"validate\":\"\"\n" +
                    "    }"),
            @Param(name = "output", type = ApiParamType.JSONARRAY, desc = "term.autoexec.outputparamlist"),
            @Param(name = "defaultProfile", type = ApiParamType.STRING, desc = ""),
            @Param(name = "importTime", type = ApiParamType.LONG, isRequired = true, desc = "common.editdate")
    })
    @Output({
    })
    @Description(desc = "nmaat.registerautoexectoolapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String opName = jsonObj.getString("opName");
        String typeName = jsonObj.getString("typeName");
        String riskName = jsonObj.getString("riskName");
        String defaultProfile = jsonObj.getString("defaultProfile");
        JSONArray option = jsonObj.getJSONArray("option");
        JSONObject argument = jsonObj.getJSONObject("argument");
        JSONArray output = jsonObj.getJSONArray("output");
        Long typeId = autoexecTypeMapper.getTypeIdByName(typeName);
        Long riskId = autoexecRiskMapper.getRiskIdByName(riskName);
        AutoexecToolVo oldTool = autoexecToolMapper.getToolByName(opName);
        if (typeId == null) {
            throw new AutoexecTypeNotFoundException(typeName);
        }
        if (riskId == null) {
            throw new AutoexecRiskNotFoundException(riskName);
        }
        JSONArray paramList = getParamList(option, output);
        AutoexecToolVo vo = new AutoexecToolVo(jsonObj);
        if (oldTool != null) {
            vo.setId(oldTool.getId());
            vo.setIsActive(oldTool.getIsActive() != null ? oldTool.getIsActive() : 1);
        } else {
            vo.setIsActive(1);
        }
        vo.setTypeId(typeId);
        vo.setRiskId(riskId);
        vo.setDefaultProfileId(autoexecService.saveProfileOperation(defaultProfile, vo.getId(), ToolType.TOOL.getValue()));
        JSONObject config = new JSONObject();
        if (CollectionUtils.isNotEmpty(paramList)) {
            config.put("paramList", paramList);
        }
        if (MapUtils.isNotEmpty(argument)) {
            config.put("argument", new AutoexecParamVo(argument));
        }
        vo.setConfigStr(config.toJSONString());
        autoexecToolMapper.insertTool(vo);

        return null;
    }

    private JSONArray getParamList(JSONArray desc, JSONArray output) {
        JSONArray paramList = new JSONArray();
        if (CollectionUtils.isNotEmpty(desc)) {
            for (int i = 0; i < desc.size(); i++) {
                JSONObject param = new JSONObject();
                JSONObject value = desc.getJSONObject(i);
                String key = value.getString("opt");
                if (StringUtils.isBlank(key)) {
                    throw new ParamNotExistsException("desc[" + i + ".opt]");
                }
                String name = value.getString("name");
                if (StringUtils.isBlank(name)) {
                    throw new ParamNotExistsException("[" + key + ".name]");
                }
                param.put("key", key);
                param.put("name", name);
//                param.put("defaultValue", value.get("defaultValue"));
                param.put("mode", ParamMode.INPUT.getValue());
                String required = value.getString("required");
                if (StringUtils.isNotBlank(required)) {
                    param.put("isRequired", Objects.equals(required, "true") ? 1 : 0);
                } else {
                    param.put("isRequired", 0);
                }
                param.put("description", value.getString("help"));
                JSONObject config = null;
                JSONArray validate = null;
                try {
                    validate = value.getJSONArray("validate");
                }catch (Exception ex){
                    throw new ParamIrregularException("option.validate", I18nUtils.getMessage("nmaat.registerautoexectoolapi.getparamlist.array"));
                }
                if (CollectionUtils.isNotEmpty(validate)) {
                    config = new JSONObject();
                    List<Object> validateList = new ArrayList<>();
                    for (int j = 0; j < validate.size(); j++) {
                        Object o = validate.get(j);
                        if (!(o instanceof String)) {
                            JSONObject regex;
                            try {
                                regex = JSONObject.parseObject(o.toString());
                            } catch (Exception ex) {
                                throw new AutoexecToolParamValidateFieldFormatIllegalException(key, o.toString());
                            }
                            if (StringUtils.isBlank(regex.getString("name"))) {
                                throw new AutoexecToolParamValidateFieldLostException(key, j + 1, "name");
                            }
                            if (StringUtils.isBlank(regex.getString("pattern"))) {
                                throw new AutoexecToolParamValidateFieldLostException(key, "name", "pattern");
                            }
                            validateList.add(o);
                        } else {
                            validateList.add(new JSONObject() {
                                {
                                    this.put("name", o);
                                }
                            });
                        }
                    }
                    config.put("validateList", validateList);
                    param.put("config", config);
                }
                String type = value.getString("type");
                Object defaultValue = value.get("defaultValue");
                JSONObject dataSource = value.getJSONObject("dataSource");
                ParamType paramType = ParamType.getParamType(type);
                if (paramType == null) {
                    throw new ParamTypeNotFoundException(type);
                }
                if (defaultValue != null && defualtValuePattern.matcher(defaultValue.toString()).matches()) {
                    Matcher matcher = defualtValuePattern.matcher(defaultValue.toString());
                    String mappingValue = null;
                    while (matcher.find()) {
                        mappingValue = matcher.group(1);
                    }
                    if (StringUtils.isBlank(mappingValue)) {
                        throw new AutoexecGlobalParamValueEmptyException(key);
                    }
                    AutoexecGlobalParamType globalParamType = AutoexecGlobalParamType.getParamType(type);
                    if (globalParamType == null) {
                        throw new AutoexecGlobalParamTypeNotFoundException(key, type);
                    }
                    if (autoexecGlobalParamMapper.getGlobalParamByKey(mappingValue) == null) {
                        // 如果不存在名为{mappingValue}的全局参数，则创建
                        AutoexecGlobalParamVo globalParamVo = new AutoexecGlobalParamVo(mappingValue, mappingValue, globalParamType.getValue());
                        autoexecGlobalParamMapper.insertGlobalParam(globalParamVo);
                    }
                    param.put("mappingMode", AutoexecProfileParamInvokeType.GLOBAL_PARAM.getValue());
                    param.put("defaultValue", mappingValue);
                } else {
                    param.put("defaultValue", defaultValue);
                    if (ScriptParamTypeFactory.getHandler(type).needDataSource()) {
                        if (MapUtils.isEmpty(dataSource)) {
                            throw new AutoexecToolParamDatasourceEmptyException(key);
                        }
                        JSONArray dataList = dataSource.getJSONArray("dataList");
                        if (CollectionUtils.isEmpty(dataList)) {
                            throw new AutoexecToolParamDatasourceIllegalException(key);
                        }
                        for (int j = 0; j < dataList.size(); j++) {
                            JSONObject data = dataList.getJSONObject(j);
                            if (StringUtils.isBlank(data.getString("text")) || !data.containsKey("value")) {
                                throw new AutoexecToolParamDatasourceIllegalException(key);
                            }
                        }
                        if (config == null) {
                            config = new JSONObject();
                        }
                        config.put("dataSource", ParamDataSource.STATIC.getValue());
                        config.put("dataList", dataList);
                        param.put("config", config);
                    }
                }
                param.put("type", type);
                param.put("sort", i);
                paramList.add(param);
            }
        }
        if (CollectionUtils.isNotEmpty(output)) {
            for (int i = 0; i < output.size(); i++) {
                JSONObject param = new JSONObject();
                JSONObject value = output.getJSONObject(i);
                String key = value.getString("opt");
                if (StringUtils.isBlank(key)) {
                    throw new ParamNotExistsException("output[" + i + ".opt]");
                }
                String name = value.getString("name");
                if (StringUtils.isBlank(name)) {
                    throw new ParamNotExistsException("[" + key + ".name]");
                }
                OutputParamType paramType;
                String type = value.getString("type");
                if (ParamType.FILE.getValue().equals(type)) {
                    paramType = OutputParamType.FILEPATH;
                } else {
                    paramType = OutputParamType.getParamType(type);
                }
                if (paramType == null) {
                    paramType = OutputParamType.TEXT;
                }
                param.put("key", key);
                param.put("name", name);
                param.put("type", paramType.getValue());
                param.put("mode", ParamMode.OUTPUT.getValue());
                param.put("defaultValue", value.get("defaultValue"));
                param.put("description", value.getString("help"));
                param.put("sort", i);
                paramList.add(param);
            }
        }
        return paramList;
    }

}
