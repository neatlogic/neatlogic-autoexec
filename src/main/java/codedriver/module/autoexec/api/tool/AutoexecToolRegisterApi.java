/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.tool;

import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.global.param.AutoexecGlobalParamVo;
import codedriver.framework.autoexec.dto.profile.AutoexecProfileVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.autoexec.script.paramtype.ScriptParamTypeFactory;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.exception.type.ParamTypeNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import codedriver.framework.util.RegexUtils;
import codedriver.module.autoexec.dao.mapper.AutoexecGlobalParamMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecProfileMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecToolRegisterApi extends PublicApiComponentBase {

    final Pattern defualtValuePattern = Pattern.compile("\\$\\{(.*):(.*)\\}");

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Resource
    private AutoexecGlobalParamMapper autoexecGbobalParamMapper;

    @Resource
    private AutoexecProfileMapper autoexecProfileMapper;

    @Override
    public String getToken() {
        return "autoexec/tool/register";
    }

    @Override
    public String getName() {
        return "注册内置工具";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "opName", type = ApiParamType.REGEX, rule = RegexUtils.NAME_WITH_SLASH, maxLength = 50, isRequired = true, desc = "工具名称"),
            @Param(name = "opType", type = ApiParamType.ENUM, rule = "runner,target,runner_target,sqlfile,native", isRequired = true, desc = "执行方式"),
            @Param(name = "typeName", type = ApiParamType.REGEX, rule = RegexUtils.NAME, maxLength = 50, isRequired = true, desc = "工具分类名称"),
            @Param(name = "riskName", type = ApiParamType.REGEX, rule = RegexUtils.NAME, maxLength = 50, isRequired = true, desc = "操作级别名称"),
            @Param(name = "interpreter", type = ApiParamType.ENUM, rule = "python,ruby,vbscript,perl,powershell,cmd,bash,ksh,csh,sh,javascript", isRequired = true, desc = "解析器"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "option", type = ApiParamType.JSONARRAY,
                    desc = "入参(当控件类型为[select,multiselect,radio,checkbox]时，需要在dataSource字段填写数据源，格式如下：[{\"text\":\"否\",\"value\":\"0\"},{\"text\":\"是\",\"value\":\"1\"}]，defaultValue字段填写数据源中的value值)"),
            @Param(name = "argument", type = ApiParamType.JSONOBJECT, desc = "{\n" +
                    "        \"name\":\"日志路径\",\n" +
                    "        \"help\":\"日志路径，支持通配符和反引号\",\n" +
                    "        \"type\":\"input\",\n" +
                    "        \"isConst\":\"false\",\n" +
                    "        \"defaultValue\":\"\",\n" +
                    "        \"required\":\"true\",\n" +
                    "        \"validate\":\"\"\n" +
                    "    }"),
            @Param(name = "output", type = ApiParamType.JSONARRAY, desc = "出参"),
    })
    @Output({
    })
    @Description(desc = "注册内置工具")
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
        if (StringUtils.isNotBlank(defaultProfile)) {
            AutoexecProfileVo profile = autoexecProfileMapper.getProfileVoByName(defaultProfile);
            if (profile == null) {
                profile = new AutoexecProfileVo(defaultProfile, -1L);
                autoexecProfileMapper.insertProfile(profile);
            }
            autoexecProfileMapper.insertAutoexecProfileOperation(profile.getId(), Collections.singletonList(vo.getId()), ToolType.TOOL.getValue(), new Date());
        }
        JSONObject config = new JSONObject();
        if (CollectionUtils.isNotEmpty(paramList)) {
            config.put("paramList", paramList);
        }
        if (MapUtils.isNotEmpty(argument)) {
            config.put("argument", new AutoexecParamVo(argument));
        }
        vo.setConfigStr(config.toJSONString());
        autoexecToolMapper.replaceTool(vo);

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
                String type = value.getString("type");
                Object defaultValue = value.get("defaultValue");
                JSONArray dataSource = value.getJSONArray("dataSource");
                ParamType paramType = ParamType.getParamType(type);
                if (paramType == null) {
                    throw new ParamTypeNotFoundException(type);
                }
                if (defaultValue != null && defualtValuePattern.matcher(defaultValue.toString()).matches()) {
                    Matcher matcher = defualtValuePattern.matcher(defaultValue.toString());
                    String mappingMode = null;
                    String mappingValue = null;
                    while (matcher.find()) {
                        mappingMode = matcher.group(1);
                        mappingValue = matcher.group(2);
                    }
                    if (mappingMode != null && mappingValue != null) {
                        String paramMappingMode = paramMappingModeMap.get(mappingMode);
                        if (paramMappingMode == null) {
                            throw new AutoexecParamMappingNotFoundException(key, mappingMode);
                        }
                        if (AutoexecProfileParamInvokeType.GLOBAL_PARAM.getValue().equals(paramMappingMode)) {
                            AutoexecGlobalParamType globalParamType = AutoexecGlobalParamType.getParamType(type);
                            if (globalParamType == null) {
                                throw new AutoexecGlobalParamTypeNotFoundException(key, type);
                            }
                            if (autoexecGbobalParamMapper.getGlobalParamByKey(mappingValue) == null) {
                                // 如果不存在名为{mappingValue}的全局参数，则创建
                                AutoexecGlobalParamVo globalParamVo = new AutoexecGlobalParamVo(mappingValue, mappingValue, globalParamType.getValue());
                                autoexecGbobalParamMapper.insertGlobalParam(globalParamVo);
                            }
                            param.put("mappingMode", AutoexecProfileParamInvokeType.GLOBAL_PARAM.getValue());
                            param.put("defaultValue", mappingValue);
                        }
                    }
                } else {
                    param.put("defaultValue", defaultValue);
                    if (ScriptParamTypeFactory.getHandler(type).needDataSource()) {
                        if (CollectionUtils.isEmpty(dataSource)) {
                            throw new AutoexecToolParamDatasourceEmptyException(key);
                        }
                        for (int j = 0; j < dataSource.size(); j++) {
                            JSONObject data = dataSource.getJSONObject(j);
                            if (StringUtils.isBlank(data.getString("text")) || !data.containsKey("value")) {
                                throw new AutoexecToolParamDatasourceIllegalException(key);
                            }
                        }
                        JSONObject config = new JSONObject();
                        config.put("dataSource", ParamDataSource.STATIC.getValue());
                        config.put("dataList", dataSource);
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

    static Map<String, String> paramMappingModeMap = new HashMap<>();

    static {
        paramMappingModeMap.put("global", AutoexecProfileParamInvokeType.GLOBAL_PARAM.getValue());
    }

}
