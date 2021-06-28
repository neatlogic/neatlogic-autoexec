/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.tool;

import codedriver.framework.autoexec.constvalue.ParamMode;
import codedriver.framework.autoexec.constvalue.ParamType;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.exception.AutoexecRiskNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecToolParamDatasourceEmptyException;
import codedriver.framework.autoexec.exception.AutoexecToolParamDatasourceFormatIllegalException;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.exception.type.ParamTypeNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecRiskMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecTypeMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

@Service
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecToolRegisterApi extends PublicApiComponentBase {

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

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
            @Param(name = "opName", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5/]+$", maxLength = 50, isRequired = true, desc = "工具名称"),
            @Param(name = "opType", type = ApiParamType.ENUM, rule = "runner,target,runner_target,sqlfile", isRequired = true, desc = "执行方式"),
            @Param(name = "typeName", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", maxLength = 50, isRequired = true, desc = "工具分类名称"),
            @Param(name = "riskName", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", maxLength = 50, isRequired = true, desc = "操作级别名称"),
            @Param(name = "interpreter", type = ApiParamType.ENUM, rule = "python,ruby,vbscript,shell,perl,powershell,cmd,bash,ksh,csh,sh,javascript,xml,sql", isRequired = true, desc = "解析器"),
            @Param(name = "description", type = ApiParamType.STRING, desc = "描述"),
            @Param(name = "desc", type = ApiParamType.JSONOBJECT,
                    desc = "入参(当控件类型为[select,multiselect,radio,checkbox]时，需要在defaultValue字段填写数据源，格式如下：[{\"text\":\"否\",\"value\":\"0\",\"selected\":\"true\"},{\"text\":\"是\",\"value\":\"1\"}])"),
            @Param(name = "output", type = ApiParamType.JSONOBJECT, desc = "出参"),
    })
    @Output({
    })
    @Description(desc = "注册内置工具")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String opName = jsonObj.getString("opName");
        String opType = jsonObj.getString("opType");
        String typeName = jsonObj.getString("typeName");
        String riskName = jsonObj.getString("riskName");
        String interpreter = jsonObj.getString("interpreter");
        String description = jsonObj.getString("description");
        JSONObject desc = jsonObj.getJSONObject("desc");
        JSONObject output = jsonObj.getJSONObject("output");
        Long typeId = autoexecTypeMapper.getTypeIdByName(typeName);
        Long riskId = autoexecRiskMapper.getRiskIdByName(riskName);
        AutoexecToolVo oldTool = autoexecToolMapper.getToolByName(opName);
        if (typeId == null) {
            throw new AutoexecTypeNotFoundException(typeName);
        }
        if (riskId == null) {
            throw new AutoexecRiskNotFoundException(riskName);
        }
        JSONArray paramList = getParamList(desc, output);
        AutoexecToolVo vo = new AutoexecToolVo();
        if (oldTool != null) {
            vo.setId(oldTool.getId());
            vo.setIsActive(oldTool.getIsActive() != null ? oldTool.getIsActive() : 1);
        } else {
            vo.setIsActive(1);
        }
        vo.setName(opName);
        vo.setExecMode(opType);
        vo.setParser(interpreter);
        vo.setTypeId(typeId);
        vo.setRiskId(riskId);
        vo.setDescription(description);
        if (CollectionUtils.isNotEmpty(paramList)) {
            JSONObject config = new JSONObject();
            config.put("paramList", paramList);
            vo.setConfigStr(config.toJSONString());
        }
        autoexecToolMapper.replaceTool(vo);

        return null;
    }

    private JSONArray getParamList(JSONObject desc, JSONObject output) {
        JSONArray paramList = new JSONArray();
        if (MapUtils.isNotEmpty(desc)) {
            Iterator<Map.Entry<String, Object>> iterator = desc.entrySet().iterator();
            int i = 0;
            while (iterator.hasNext()) {
                JSONObject param = new JSONObject();
                Map.Entry<String, Object> next = iterator.next();
                String key = next.getKey();
                JSONObject value = (JSONObject) next.getValue();
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
                if (Objects.equals(type, "input")) {
                    type = ParamType.TEXT.getValue();
                } else {
                    ParamType paramType = ParamType.getParamType(type);
                    if (paramType != null) {
                        type = paramType.getValue();
                        if (paramType.getNeedDataSource()) {
                            if (defaultValue == null) {
                                throw new AutoexecToolParamDatasourceEmptyException(key);
                            }
                            JSONArray list;
                            try {
                                list = JSONArray.parseArray(defaultValue.toString());
                            } catch (JSONException ex) {
                                throw new AutoexecToolParamDatasourceFormatIllegalException(key);
                            }
                            JSONArray defaultValueList = new JSONArray();
                            JSONArray dataList = new JSONArray();
                            for (Object o : list) {
                                JSONObject object = (JSONObject) o;
                                if (Objects.equals(object.getString("selected"), "true")) {
                                    defaultValueList.add(object.get("value"));
                                    object.remove("selected");
                                }
                                dataList.add(object);
                            }
                            if (defaultValueList.size() > 0) {
                                if (Objects.equals(paramType, ParamType.SELECT) || Objects.equals(paramType, ParamType.RADIO)) {
                                    defaultValue = defaultValueList.get(0);
                                } else if (Objects.equals(paramType, ParamType.MULTISELECT) || Objects.equals(paramType, ParamType.CHECKBOX)) {
                                    defaultValue = defaultValueList;
                                }
                            }
                            JSONObject config = new JSONObject();
                            config.put("dataSource", "static");
                            config.put("dataList", dataList);
                            param.put("config", config);
                        }
                    } else {
                        throw new ParamTypeNotFoundException(type);
                    }
                }
                param.put("defaultValue", defaultValue);
                param.put("type", type);
                param.put("sort", i++);
                paramList.add(param);
            }
        }
        if (MapUtils.isNotEmpty(output)) {
            Iterator<Map.Entry<String, Object>> iterator = output.entrySet().iterator();
            int i = 0;
            while (iterator.hasNext()) {
                JSONObject param = new JSONObject();
                Map.Entry<String, Object> next = iterator.next();
                String key = next.getKey();
                JSONObject value = (JSONObject) next.getValue();
                String name = value.getString("name");
                if (StringUtils.isBlank(name)) {
                    throw new ParamNotExistsException("[" + key + ".name]");
                }
                param.put("key", key);
                param.put("name", name);
                param.put("type", ParamType.TEXT.getValue());
                param.put("mode", ParamMode.OUTPUT.getValue());
                param.put("defaultValue", value.get("defaultValue"));
                param.put("description", value.getString("help"));
                param.put("sort", i++);
                paramList.add(param);
            }
        }
        return paramList;
    }


}
