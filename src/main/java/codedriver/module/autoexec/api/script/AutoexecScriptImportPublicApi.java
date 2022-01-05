package codedriver.module.autoexec.api.script;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.ScriptParser;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import codedriver.framework.fulltextindex.core.IFullTextIndexHandler;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicJsonStreamApiComponentBase;
import codedriver.module.autoexec.fulltextindex.AutoexecFullTextIndexType;
import codedriver.module.autoexec.service.AutoexecScriptService;
import codedriver.module.autoexec.service.AutoexecService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
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

        // 重名拒绝导入
        // 部分参数不支持，如单选、复选、下拉、节点、账号，文件参数不支持默认值
        JSONArray faultArray = new JSONArray();
        jsonReader.startArray();
        int i = 1;
        while (jsonReader.hasNext()) {
            List<String> faultMessages = new ArrayList<>();
            AutoexecScriptVo scriptVo = jsonReader.readObject(AutoexecScriptVo.class);
            if (StringUtils.isBlank(scriptVo.getName())) {
                faultMessages.add("自定义工具名称为空");
            }
            if (StringUtils.isBlank(scriptVo.getCatalogName())) {
                faultMessages.add("工具目录为空");
            }
            if (StringUtils.isBlank(scriptVo.getRiskName())) {
                faultMessages.add("操作级别为空");
            }
            if (StringUtils.isBlank(scriptVo.getTypeName())) {
                faultMessages.add("工具分类为空");
            }
            if (StringUtils.isBlank(scriptVo.getExecMode())) {
                faultMessages.add("执行方式为空");
            }
            if (StringUtils.isBlank(scriptVo.getParser())) {
                faultMessages.add("脚本解析器为空");
            }
            if (CollectionUtils.isEmpty(scriptVo.getLineList())) {
                faultMessages.add("脚本内容为空");
            }
            if (StringUtils.isNotBlank(scriptVo.getName()) && autoexecScriptMapper.checkScriptNameIsExists(scriptVo) > 0) {
                faultMessages.add("自定义工具：'" + scriptVo.getName() + "'已存在");
            }
            if (StringUtils.isNotBlank(scriptVo.getCatalogName()) && autoexecCatalogMapper.getAutoexecCatalogByName(scriptVo.getCatalogName()) == null) {
                faultMessages.add("工具目录：'" + scriptVo.getCatalogName() + "'不存在");
            }
            if (StringUtils.isNotBlank(scriptVo.getTypeName()) && autoexecTypeMapper.getTypeIdByName(scriptVo.getTypeName()) == null) {
                faultMessages.add("工具分类：'" + scriptVo.getTypeName() + "'不存在");
            }
            if (StringUtils.isNotBlank(scriptVo.getRiskName()) && autoexecRiskMapper.getRiskIdByName(scriptVo.getRiskName()) == null) {
                faultMessages.add("操作级别：'" + scriptVo.getRiskName() + "'不存在");
            }
            if (StringUtils.isNotBlank(scriptVo.getExecMode()) && ExecMode.getExecMode(scriptVo.getExecMode()) == null) {
                faultMessages.add("执行方式：'" + scriptVo.getExecMode() + "'不存在");
            }
            if (StringUtils.isNotBlank(scriptVo.getParser()) && ScriptParser.getScriptParser(scriptVo.getParser()) == null) {
                faultMessages.add("脚本解析器：'" + scriptVo.getParser() + "'不存在");
            }
            if (CollectionUtils.isNotEmpty(scriptVo.getParamList())) {
                try {
                    autoexecService.validateParamList(scriptVo.getParamList());
                } catch (Exception ex) {
                    faultMessages.add(ex.getMessage());
                }
            }
            if (faultMessages.isEmpty()) {
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
                versionVo.setParamList(scriptVo.getParamList());
                versionVo.setLineList(scriptVo.getLineList());
                autoexecScriptMapper.insertScript(scriptVo);
                autoexecScriptMapper.insertScriptVersion(versionVo);
                autoexecScriptService.saveParamList(versionVo.getId(), versionVo.getParamList());
                autoexecScriptService.saveLineList(scriptVo.getId(), versionVo.getId(), versionVo.getLineList());
                IFullTextIndexHandler fullTextIndexHandler = FullTextIndexHandlerFactory.getHandler(AutoexecFullTextIndexType.SCRIPT_DOCUMENT_VERSION);
                if (fullTextIndexHandler != null) {
                    fullTextIndexHandler.createIndex(versionVo.getId());
                }
            } else {
                JSONObject faultObj = new JSONObject();
                String item;
                if (StringUtils.isNotBlank(scriptVo.getName())) {
                    item = "导入" + scriptVo.getName() + "失败";
                } else {
                    item = "导入第[" + i + "]个失败";
                }
                faultObj.put("item", item);
                faultObj.put("faultMessages", faultMessages);
                faultArray.add(faultObj);
            }
            i++;
        }
        jsonReader.endArray();
        jsonReader.close();
        return faultArray;
    }
}
