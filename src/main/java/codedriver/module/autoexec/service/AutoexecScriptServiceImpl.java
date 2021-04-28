/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.ParamMode;
import codedriver.framework.autoexec.constvalue.ParamType;
import codedriver.framework.autoexec.dto.script.AutoexecScriptAuditContentVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptAuditVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptLineVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecRiskNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecScriptNameOrUkRepeatException;
import codedriver.framework.autoexec.exception.AutoexecScriptVersionNotFoundException;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.exception.type.ParamIrregularException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.module.autoexec.dao.mapper.AutoexecRiskMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecTypeMapper;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AutoexecScriptServiceImpl implements AutoexecScriptService {

    static Pattern paramKeyPattern = Pattern.compile("^[A-Za-z_\\d\\u4e00-\\u9fa5]+$");

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    /**
     * 获取脚本版本详细信息，包括参数与脚本内容
     *
     * @param versionId 版本ID
     * @return 脚本版本VO
     */
    @Override
    public AutoexecScriptVersionVo getScriptVersionDetailByVersionId(Long versionId) {
        AutoexecScriptVersionVo version = autoexecScriptMapper.getVersionByVersionId(versionId);
        if (version == null) {
            throw new AutoexecScriptVersionNotFoundException(versionId);
        }
        version.setParamList(autoexecScriptMapper.getParamListByVersionId(versionId));
        version.setLineList(autoexecScriptMapper.getLineListByVersionId(versionId));
        return version;
    }

    @Override
    public List<AutoexecScriptVersionVo> getScriptVersionDetailListByScriptId(Long scriptId) {
        List<AutoexecScriptVersionVo> versionList = autoexecScriptMapper.getVersionListByScriptId(scriptId);
        if (CollectionUtils.isNotEmpty(versionList)) {
            for (AutoexecScriptVersionVo vo : versionList) {
                vo.setParamList(autoexecScriptMapper.getParamListByVersionId(vo.getId()));
                vo.setLineList(autoexecScriptMapper.getLineListByVersionId(vo.getId()));
            }
        }
        return versionList;
    }

    /**
     * 校验脚本的基本信息，包括name、uk、分类、操作级别
     *
     * @param scriptVo 脚本VO
     */
    @Override
    public void validateScriptBaseInfo(AutoexecScriptVo scriptVo) {
        if (autoexecScriptMapper.checkScriptNameIsExists(scriptVo) > 0) {
            throw new AutoexecScriptNameOrUkRepeatException(scriptVo.getName());
        }
//        if (autoexecScriptMapper.checkScriptUkIsExists(scriptVo) > 0) {
//            throw new AutoexecScriptNameOrUkRepeatException(scriptVo.getName());
//        }
        if (autoexecTypeMapper.checkTypeIsExistsById(scriptVo.getTypeId()) == 0) {
            throw new AutoexecTypeNotFoundException(scriptVo.getTypeId());
        }
        if (autoexecRiskMapper.checkRiskIsExistsById(scriptVo.getRiskId()) == 0) {
            throw new AutoexecRiskNotFoundException(scriptVo.getRiskId());
        }
    }

    /**
     * 校验参数列表
     *
     * @param paramList
     */
    @Override
    public void validateParamList(List<AutoexecScriptVersionParamVo> paramList) {
        for (int i = 0; i < paramList.size(); i++) {
            AutoexecScriptVersionParamVo param = paramList.get(i);
            if (param != null) {
                String key = param.getKey();
                if (StringUtils.isBlank(key)) {
                    throw new ParamNotExistsException("参数：“paramList.[" + i + "].key”不能为空");
                }
                if (!paramKeyPattern.matcher(key).matches()) {
                    throw new ParamIrregularException("参数：“paramList.[" + i + "].key”不符合格式要求");
                }
                Integer isRequired = param.getIsRequired();
                if (isRequired == null && ParamMode.INPUT.getValue().equals(param.getMode())) {
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

    /**
     * 批量插入脚本参数
     *
     * @param paramList 参数列表
     * @param batchSize 每批的数量
     */
    @Override
    public void batchInsertScriptVersionParamList(List<AutoexecScriptVersionParamVo> paramList, int batchSize) {
        if (CollectionUtils.isNotEmpty(paramList)) {
            int begin = 0;
            int end = begin + batchSize;
            while (paramList.size() - 1 >= begin) {
                autoexecScriptMapper.insertScriptVersionParamList(paramList.subList(begin, paramList.size() >= end ? end : paramList.size()));
                begin = end;
                end = begin + batchSize;
            }
        }
    }

    /**
     * 批量插入脚本内容行
     *
     * @param lineList  内容行列表
     * @param batchSize 每批的数量
     */
    @Override
    public void batchInsertScriptLineList(List<AutoexecScriptLineVo> lineList, int batchSize) {
        if (CollectionUtils.isNotEmpty(lineList)) {
            int begin = 0;
            int end = begin + batchSize;
            while (lineList.size() - 1 >= begin) {
                autoexecScriptMapper.insertScriptLineList(lineList.subList(begin, lineList.size() >= end ? end : lineList.size()));
                begin = end;
                end = begin + batchSize;
            }
        }
    }

    /**
     * 记录活动
     *
     * @param auditVo 活动VO
     */
    @Override
    public void audit(AutoexecScriptAuditVo auditVo) {
        auditVo.setFcu(UserContext.get().getUserUuid());
        if (MapUtils.isNotEmpty(auditVo.getConfig())) {
            AutoexecScriptAuditContentVo contentVo = new AutoexecScriptAuditContentVo(auditVo.getConfig().toJSONString());
            autoexecScriptMapper.insertScriptAuditDetail(contentVo);
            auditVo.setContentHash(contentVo.getHash());
        }
        autoexecScriptMapper.insertScriptAudit(auditVo);
    }
}
