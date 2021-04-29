/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.constvalue.ParamMode;
import codedriver.framework.autoexec.constvalue.ParamType;
import codedriver.framework.autoexec.dto.IParam;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.exception.type.ParamNotExistsException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
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
    public void validateParamList(List<? extends IParam> paramList) {
        for (int i = 0; i < paramList.size(); i++) {
            IParam param = paramList.get(i);
            if (param != null) {
                String key = param.getKey();
                if (StringUtils.isBlank(key)) {
                    throw new ParamNotExistsException("参数：“paramList.[" + i + "].key”不能为空");
                }
                if (!paramKeyPattern.matcher(key).matches()) {
                    throw new ParamIrregularException("参数：“paramList.[" + i + "].key”不符合格式要求");
                }
                Integer isRequired = param.getIsRequired();
                if (isRequired == null && !ParamMode.OUTPUT.getValue().equals(param.getMode())) {
                    throw new ParamNotExistsException("参数：“paramList.[" + i + "].isRequired”不能为空");
                }
                String type = param.getType();
                if (StringUtils.isBlank(type)) {
                    throw new ParamNotExistsException("参数：“paramList.[" + i + "].type”不能为空");
                }
                ParamType paramType = ParamType.getParamType(type);
                if (paramType == null) {
                    throw new ParamIrregularException("参数：“paramList.[" + i + "].type”不符合格式要求");
                }
                if (ParamType.TEXT != paramType && ParamMode.OUTPUT.getValue().equals(param.getMode())) {
                    throw new ParamIrregularException("输出参数：“paramList.[" + i + "].type”必须是文本类型");
                }
            }
        }

    }

}
