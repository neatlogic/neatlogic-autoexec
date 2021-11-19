/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.ParamType;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopParamVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.CiphertextPrefix;
import codedriver.framework.common.util.RC4Util;
import codedriver.framework.dependency.core.DependencyManager;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.dependency.MatrixAutoexecCombopParamDependencyHandler;
import codedriver.module.autoexec.service.AutoexecCombopService;
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
import java.util.regex.Pattern;

/**
 * 保存组合工具运行参数接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class AutoexecCombopParamSaveApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

    @Override
    public String getToken() {
        return "autoexec/combop/param/save";
    }

    @Override
    public String getName() {
        return "保存组合工具运行参数";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "combopId", type = ApiParamType.LONG, isRequired = true, desc = "组合工具主键id"),
            @Param(name = "paramList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "参数列表[{\"key\": \"参数名\", \"name\": \"中文名\", \"defaultValue\": \"默认值\", \"description\": \"描述\", \"isRequired\": \"是否必填\", \"type\": \"参数类型\"}]")
    })
    @Description(desc = "保存组合工具运行参数")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long combopId = jsonObj.getLong("combopId");
        AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(combopId);
        if (autoexecCombopVo == null) {
            throw new AutoexecCombopNotFoundException(combopId);
        }
        autoexecCombopService.setOperableButtonList(autoexecCombopVo);
        if (Objects.equals(autoexecCombopVo.getEditable(), 0)) {
            throw new PermissionDeniedException();
        }
        List<AutoexecCombopParamVo> autoexecCombopParamList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(combopId);
        if (CollectionUtils.isNotEmpty(autoexecCombopParamList)) {
            autoexecCombopMapper.deleteAutoexecCombopParamByCombopId(combopId);
            DependencyManager.delete(MatrixAutoexecCombopParamDependencyHandler.class, combopId);
        }

        List<AutoexecCombopParamVo> autoexecCombopParamVoList = new ArrayList<>();
        Pattern keyPattern = Pattern.compile("^[A-Za-z_\\d]+$");
        Pattern namePattern = Pattern.compile("^[A-Za-z_\\d\\u4e00-\\u9fa5]+$");
        JSONArray paramList = jsonObj.getJSONArray("paramList");
        for (int i = 0; i < paramList.size(); i++) {
            AutoexecCombopParamVo autoexecCombopParamVo = paramList.getObject(i, AutoexecCombopParamVo.class);
            if (autoexecCombopParamVo != null) {
                String key = autoexecCombopParamVo.getKey();
                if (StringUtils.isBlank(key)) {
                    throw new ParamNotExistsException("paramList.[" + i + "].key");
                }
                if (!keyPattern.matcher(key).matches()) {
                    throw new ParamIrregularException("paramList.[" + i + "].key");
                }
                String name = autoexecCombopParamVo.getName();
                if (StringUtils.isBlank(name)) {
                    throw new ParamNotExistsException("paramList.[" + i + "].name");
                }
                if (!namePattern.matcher(name).matches()) {
                    throw new ParamIrregularException("paramList.[" + i + "].name");
                }
                Integer isRequired = autoexecCombopParamVo.getIsRequired();
                if (isRequired == null) {
                    throw new ParamNotExistsException("paramList.[" + i + "].isRequired");
                }
                String type = autoexecCombopParamVo.getType();
                if (StringUtils.isBlank(type)) {
                    throw new ParamNotExistsException("paramList.[" + i + "].type");
                }
                ParamType paramType = ParamType.getParamType(type);
                if (paramType == null) {
                    throw new ParamIrregularException("paramList.[" + i + "].type");
                }
                Object value = autoexecCombopParamVo.getDefaultValue();
                // 如果默认值不以"RC4:"开头，说明修改了密码，则重新加密
                if (paramType == ParamType.PASSWORD && value != null && !value.toString().startsWith(CiphertextPrefix.RC4.getValue())) {
                    autoexecCombopParamVo.setDefaultValue(CiphertextPrefix.RC4.getValue() + RC4Util.encrypt((String) value));
                } else if (paramType == ParamType.SELECT || paramType == ParamType.MULTISELECT || paramType == ParamType.CHECKBOX || paramType == ParamType.RADIO) {
                    JSONObject config = autoexecCombopParamVo.getConfig();
                    if (MapUtils.isNotEmpty(config)) {
                        String matrixUuid = config.getString("matrixUuid");
                        if (StringUtils.isNotBlank(matrixUuid)) {
                            JSONArray callers = new JSONArray();
                            callers.add(combopId);
                            callers.add(key);
                            DependencyManager.insert(MatrixAutoexecCombopParamDependencyHandler.class, matrixUuid, callers);
                        }
                    }
                }
                autoexecCombopParamVo.setCombopId(combopId);
                autoexecCombopParamVo.setSort(i);
                autoexecCombopParamVoList.add(autoexecCombopParamVo);
                if (autoexecCombopParamVoList.size() == 1000) {
                    autoexecCombopMapper.insertAutoexecCombopParamVoList(autoexecCombopParamVoList);
                    autoexecCombopParamVoList.clear();
                }
            }
        }
        if (CollectionUtils.isNotEmpty(autoexecCombopParamVoList)) {
            autoexecCombopMapper.insertAutoexecCombopParamVoList(autoexecCombopParamVoList);
        }
        return null;
    }

}
