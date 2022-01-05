package codedriver.module.autoexec.api.script;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import codedriver.framework.fulltextindex.core.IFullTextIndexHandler;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicJsonStreamApiComponentBase;
import codedriver.module.autoexec.fulltextindex.AutoexecFullTextIndexType;
import codedriver.module.autoexec.service.AutoexecScriptService;
import codedriver.module.autoexec.service.AutoexecService;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@SuppressWarnings("deprecation")
@Service
@Transactional
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecScriptImportPublicApi extends PublicJsonStreamApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Resource
    private AutoexecService autoexecService;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/import/fromjson";
    }

    @Override
    public String getName() {
        return "导入脚本(通过固定格式json文件)";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Description(desc = "导入脚本(通过固定格式json文件)")
    @Override
    public Object myDoService(JSONObject paramObj, JSONReader jsonReader) throws Exception {

        // todo 重名怎么办
        // todo 部分参数不支持，如单选、复选、下拉、节点、账号，文件参数不支持默认值
        // todo 版本状态
        // todo 导入者
        jsonReader.startArray();
        while (jsonReader.hasNext()) {
            AutoexecScriptVo scriptVo = jsonReader.readObject(AutoexecScriptVo.class);
            validateBaseInfo(scriptVo);
            List<AutoexecScriptVersionParamVo> paramList = scriptVo.getParamList();
            if (CollectionUtils.isNotEmpty(paramList)) {
                autoexecService.validateParamList(paramList);
            }
            AutoexecCatalogVo catalog = autoexecCatalogMapper.getAutoexecCatalogByName(scriptVo.getCatalogName());
            Long typeId = autoexecTypeMapper.getTypeIdByName(scriptVo.getTypeName());
            Long riskId = autoexecRiskMapper.getRiskIdByName(scriptVo.getRiskName());
            scriptVo.setCatalogId(catalog.getId());
            scriptVo.setTypeId(typeId);
            scriptVo.setRiskId(riskId);
            scriptVo.setFcu(UserContext.get().getUserUuid());
            AutoexecScriptVersionVo versionVo = new AutoexecScriptVersionVo();
            versionVo.setScriptId(scriptVo.getId());
            versionVo.setTitle(scriptVo.getName());
            versionVo.setParser(scriptVo.getParser());
            versionVo.setIsActive(0);
            versionVo.setLcu(UserContext.get().getUserUuid());
            versionVo.setStatus(ScriptVersionStatus.DRAFT.getValue());
            autoexecScriptMapper.insertScript(scriptVo);
            autoexecScriptMapper.insertScriptVersion(versionVo);
            autoexecScriptService.saveParamList(versionVo.getId(), versionVo.getParamList());
            autoexecScriptService.saveLineList(scriptVo.getId(), versionVo.getId(), versionVo.getLineList());
            IFullTextIndexHandler fullTextIndexHandler = FullTextIndexHandlerFactory.getHandler(AutoexecFullTextIndexType.SCRIPT_DOCUMENT_VERSION);
            if (fullTextIndexHandler != null) {
                fullTextIndexHandler.createIndex(versionVo.getId());
            }
        }
        jsonReader.endArray();
        jsonReader.close();
        return null;
    }

    private void validateBaseInfo(AutoexecScriptVo scriptVo) {
        if (StringUtils.isBlank(scriptVo.getName())) {
            throw new ParamNotExistsException("name");
        }
        if (StringUtils.isBlank(scriptVo.getCatalogName())) {
            throw new ParamNotExistsException("catalogName");
        }
        if (StringUtils.isBlank(scriptVo.getRiskName())) {
            throw new ParamNotExistsException("riskName");
        }
        if (StringUtils.isBlank(scriptVo.getTypeName())) {
            throw new ParamNotExistsException("typeName");
        }
        if (StringUtils.isBlank(scriptVo.getExecMode())) {
            throw new ParamNotExistsException("execMode");
        }
        if (StringUtils.isBlank(scriptVo.getParser())) {
            throw new ParamNotExistsException("parser");
        }
        if (autoexecScriptMapper.checkScriptNameIsExists(scriptVo) > 0) {
            throw new AutoexecScriptNameOrUkRepeatException(scriptVo.getName());
        }
        if (autoexecCatalogMapper.getAutoexecCatalogByName(scriptVo.getCatalogName()) == null) {
            throw new AutoexecCatalogNotFoundException(scriptVo.getCatalogName());
        }
        if (autoexecTypeMapper.getTypeIdByName(scriptVo.getTypeName()) == null) {
            throw new AutoexecTypeNotFoundException(scriptVo.getTypeName());
        }
        if (autoexecRiskMapper.getRiskIdByName(scriptVo.getRiskName()) == null) {
            throw new AutoexecRiskNotFoundException(scriptVo.getRiskName());
        }
        if (ExecMode.getExecMode(scriptVo.getExecMode()) == null) {
            throw new AutoexecExecModeNotFoundException(scriptVo.getExecMode());
        }
    }
}
