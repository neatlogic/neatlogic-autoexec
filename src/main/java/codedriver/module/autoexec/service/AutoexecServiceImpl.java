/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.constvalue.ParamMode;
import codedriver.framework.autoexec.constvalue.ParamType;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.exception.AutoexecParamFieldNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecParamIrregularException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class AutoexecServiceImpl implements AutoexecService {

    static Pattern paramKeyPattern = Pattern.compile("^[A-Za-z_\\d\\u4e00-\\u9fa5]+$");

    /**
     * 校验参数列表
     *
     * @param paramList
     */
    @Override
    public void validateParamList(List<? extends AutoexecParamVo> paramList) {
        for (int i = 0; i < paramList.size(); i++) {
            AutoexecParamVo param = paramList.get(i);
            if (param != null) {
                String mode = param.getMode();
                String key = param.getKey();
                String name = param.getName();
                String type = param.getType();
                Integer isRequired = param.getIsRequired();
                if (param instanceof AutoexecScriptVersionParamVo && StringUtils.isBlank(mode)) {
                    throw new AutoexecParamFieldNotFoundException(i + 1, "mode");
                }
                if (StringUtils.isNotBlank(mode) && ParamMode.getParamMode(mode) == null) {
                    throw new AutoexecParamIrregularException(i + 1, "mode");
                }
                if (StringUtils.isBlank(key)) {
                    throw new AutoexecParamFieldNotFoundException(i + 1, "key");
                }
                if (StringUtils.isBlank(name)) {
                    throw new AutoexecParamFieldNotFoundException(i + 1, "name");
                }
                if (!paramKeyPattern.matcher(key).matches()) {
                    throw new AutoexecParamIrregularException(i + 1, "key");
                }
                if (isRequired == null && !Objects.equals(ParamMode.OUTPUT.getValue(), mode)) {
                    throw new AutoexecParamFieldNotFoundException(i + 1, "isRequired");
                }
                if (StringUtils.isBlank(type)) {
                    throw new AutoexecParamFieldNotFoundException(i + 1, "type");
                }
                ParamType paramType = ParamType.getParamType(type);
                if (paramType == null) {
                    throw new AutoexecParamIrregularException(i + 1, "type");
                }
                if (ParamType.TEXT != paramType && ParamMode.OUTPUT.getValue().equals(param.getMode())) {
                    throw new AutoexecParamIrregularException(i + 1, "type");
                }
            }
        }

    }

}
