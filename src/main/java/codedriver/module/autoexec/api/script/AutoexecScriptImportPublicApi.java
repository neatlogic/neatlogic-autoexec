package codedriver.module.autoexec.api.script;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.*;
import codedriver.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.script.AutoexecScriptArgumentVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.fulltextindex.core.FullTextIndexHandlerFactory;
import codedriver.framework.fulltextindex.core.IFullTextIndexHandler;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateJsonStreamApiComponentBase;
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
import java.util.*;

@SuppressWarnings("deprecation")
@Service
@Transactional
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecScriptImportPublicApi extends PrivateJsonStreamApiComponentBase {

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

        // 根据名称判断脚本存不存在，如果存在且内容有变化就生成新的激活版本，不存在直接生成新的激活版本
        // todo 用户令牌可用之后，要根据导入用户决定是否自动审核通过
        JSONObject result = new JSONObject();
        JSONArray faultArray = new JSONArray();
        Set<String> newScriptArray = new HashSet<>(); // 新增的脚本
        Set<String> updatedScriptArray = new HashSet<>(); // 更新了基本信息或生成了新版本的脚本
        result.put("faultArray", faultArray);
        result.put("newScriptArray", newScriptArray);
        result.put("updatedScriptArray", updatedScriptArray);
        jsonReader.startArray();
        int i = 1;
        while (jsonReader.hasNext()) {
            List<String> faultMessages = new ArrayList<>();
            AutoexecScriptVo newScriptVo = jsonReader.readObject(AutoexecScriptVo.class);
            if (StringUtils.isBlank(newScriptVo.getName())) {
                faultMessages.add("自定义工具名称为空");
            }
            if (StringUtils.isBlank(newScriptVo.getCatalogName())) {
                faultMessages.add("工具目录为空");
            }
            if (StringUtils.isBlank(newScriptVo.getRiskName())) {
                faultMessages.add("操作级别为空");
            }
            if (StringUtils.isBlank(newScriptVo.getTypeName())) {
                faultMessages.add("工具分类为空");
            }
            if (StringUtils.isBlank(newScriptVo.getExecMode())) {
                faultMessages.add("执行方式为空");
            }
            if (StringUtils.isBlank(newScriptVo.getParser())) {
                faultMessages.add("脚本解析器为空");
            }
            if (CollectionUtils.isEmpty(newScriptVo.getLineList())) {
                faultMessages.add("脚本内容为空");
            }
            if (StringUtils.isNotBlank(newScriptVo.getTypeName()) && autoexecTypeMapper.getTypeIdByName(newScriptVo.getTypeName()) == null) {
                faultMessages.add("工具分类：'" + newScriptVo.getTypeName() + "'不存在");
            }
            if (StringUtils.isNotBlank(newScriptVo.getCatalogName()) && autoexecCatalogMapper.getAutoexecCatalogByName(newScriptVo.getCatalogName()) == null) {
                faultMessages.add("工具目录：'" + newScriptVo.getCatalogName() + "'不存在");
            }
            if (StringUtils.isNotBlank(newScriptVo.getRiskName()) && autoexecRiskMapper.getRiskIdByName(newScriptVo.getRiskName()) == null) {
                faultMessages.add("操作级别：'" + newScriptVo.getRiskName() + "'不存在");
            }
            if (StringUtils.isNotBlank(newScriptVo.getExecMode()) && ScriptExecMode.getExecMode(newScriptVo.getExecMode()) == null) {
                faultMessages.add("执行方式：'" + newScriptVo.getExecMode() + "'不存在");
            }
            if (StringUtils.isNotBlank(newScriptVo.getParser()) && ScriptParser.getScriptParser(newScriptVo.getParser()) == null) {
                faultMessages.add("脚本解析器：'" + newScriptVo.getParser() + "'不存在");
            }
            if (newScriptVo.getArgument() != null) {
                try {
                    autoexecService.validateArgument(newScriptVo.getArgument());
                } catch (Exception ex) {
                    faultMessages.add(ex.getMessage());
                }
            }
            if (CollectionUtils.isNotEmpty(newScriptVo.getParamList())) {
                try {
                    autoexecService.validateParamList(newScriptVo.getParamList());
                } catch (ApiRuntimeException ex) {
                    faultMessages.add(ex.getMessage());
                } catch (Exception ex) {
                    faultMessages.add(ex.getMessage());
                }
            }
            if (faultMessages.isEmpty()) {
                newScriptVo.setTypeId(autoexecTypeMapper.getTypeIdByName(newScriptVo.getTypeName()));
                newScriptVo.setRiskId(autoexecRiskMapper.getRiskIdByName(newScriptVo.getRiskName()));
                newScriptVo.setCatalogId(autoexecCatalogMapper.getAutoexecCatalogByName(newScriptVo.getCatalogName()).getId());

                AutoexecScriptVo oldScriptVo = autoexecScriptMapper.getScriptBaseInfoByName(newScriptVo.getName());
                Long scriptId = oldScriptVo != null ? oldScriptVo.getId() : newScriptVo.getId();
                newScriptVo.setDefaultProfileId(autoexecService.saveProfileOperation(newScriptVo.getDefaultProfileName(), scriptId, ToolType.SCRIPT.getValue()));
                if (oldScriptVo == null) {
                    newScriptArray.add(newScriptVo.getName());
                    newScriptVo.setFcu(UserContext.get().getUserUuid());
                    AutoexecScriptVersionVo versionVo = getVersionVo(newScriptVo, 1);
                    autoexecScriptMapper.insertScript(newScriptVo);
                    autoexecScriptMapper.insertScriptVersion(versionVo);
                    if (versionVo.getArgument() != null) {
                        autoexecScriptMapper.insertScriptVersionArgument(versionVo.getArgument());
                    }
                    autoexecScriptService.saveParamList(versionVo.getId(), versionVo.getParamList());
                    autoexecScriptService.saveLineList(newScriptVo.getId(), versionVo.getId(), versionVo.getLineList());
                    IFullTextIndexHandler fullTextIndexHandler = FullTextIndexHandlerFactory.getHandler(AutoexecFullTextIndexType.SCRIPT_DOCUMENT_VERSION);
                    if (fullTextIndexHandler != null) {
                        fullTextIndexHandler.createIndex(versionVo.getId());
                    }
                } else {
                    newScriptVo.setId(oldScriptVo.getId());
                    if (checkBaseInfoHasBeenChanged(newScriptVo, oldScriptVo)) {
                        autoexecScriptMapper.updateScriptBaseInfo(newScriptVo);
                        updatedScriptArray.add(newScriptVo.getName());
                    }
                    Integer maxVersion = autoexecScriptMapper.getMaxVersionByScriptId(oldScriptVo.getId());
                    AutoexecScriptVersionVo newVersionVo = getVersionVo(newScriptVo, maxVersion != null ? maxVersion + 1 : 1);
                    AutoexecScriptVersionVo oldVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(oldScriptVo.getId());
                    boolean needUpdate = true;
                    if (oldVersionVo != null) {
                        oldVersionVo.setArgument(autoexecScriptMapper.getArgumentByVersionId(oldVersionVo.getId()));
                        List<AutoexecScriptVersionParamVo> oldParamList = autoexecScriptMapper.getParamListByVersionId(oldVersionVo.getId());
                        oldVersionVo.setParamList(oldParamList);
                        oldVersionVo.setLineList(autoexecScriptMapper.getLineListByVersionId(oldVersionVo.getId()));
                        adjustParamConfig(oldParamList);
                        if (!autoexecScriptService.checkScriptVersionNeedToUpdate(oldVersionVo, newVersionVo)) {
                            needUpdate = false;
                        } else {
                            oldVersionVo.setIsActive(0);
                            oldVersionVo.setLcu(UserContext.get().getUserUuid());
                            autoexecScriptMapper.updateScriptVersion(oldVersionVo);
                        }
                    }
                    if (needUpdate) {
                        updatedScriptArray.add(newScriptVo.getName());
                        if (newVersionVo.getArgument() != null) {
                            autoexecScriptMapper.insertScriptVersionArgument(newVersionVo.getArgument());
                        }
                        autoexecScriptService.saveParamList(newVersionVo.getId(), newVersionVo.getParamList());
                        autoexecScriptService.saveLineList(newScriptVo.getId(), newVersionVo.getId(), newVersionVo.getLineList());
                        autoexecScriptMapper.insertScriptVersion(newVersionVo);
                        IFullTextIndexHandler fullTextIndexHandler = FullTextIndexHandlerFactory.getHandler(AutoexecFullTextIndexType.SCRIPT_DOCUMENT_VERSION);
                        if (fullTextIndexHandler != null) {
                            fullTextIndexHandler.createIndex(newVersionVo.getId());
                        }
                    }
                }
            } else {
                JSONObject faultObj = new JSONObject();
                String item;
                if (StringUtils.isNotBlank(newScriptVo.getName())) {
                    item = "导入" + newScriptVo.getName() + "失败";
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
        return result;
    }

    /**
     * 从autoexecscripts导入而来的脚本，参数中的config与系统的config结构不完全一致，
     * 可能导致对比时误判，故校正原有的config，使其与导入的config保持一致
     *
     * @param oldParamList
     */
    private void adjustParamConfig(List<AutoexecScriptVersionParamVo> oldParamList) {
        if (oldParamList.size() > 0) {
            for (AutoexecScriptVersionParamVo paramVo : oldParamList) {
                JSONObject config = paramVo.getConfig();
                if (config != null) {
                    if (needDataSourceTypeList.contains(paramVo.getType()) && ParamMode.INPUT.getValue().equals(paramVo.getMode())) {
                        config.remove("type");
                        config.remove("isReuqired");
                        config.remove("defaulValue");
                    } else {
                        paramVo.setConfig(null);
                    }
                }
            }
        }
    }

    /**
     * 检查脚本基本信息是否变更
     *
     * @param newScriptVo 导入的脚本
     * @param oldScriptVo 系统中的脚本
     * @return
     */
    private boolean checkBaseInfoHasBeenChanged(AutoexecScriptVo newScriptVo, AutoexecScriptVo oldScriptVo) {
        if (!Objects.equals(newScriptVo.getCatalogName(), oldScriptVo.getCatalogName())) {
            return true;
        }
        if (!Objects.equals(newScriptVo.getTypeName(), oldScriptVo.getTypeName())) {
            return true;
        }
        if (!Objects.equals(newScriptVo.getExecMode(), oldScriptVo.getExecMode())) {
            return true;
        }
        if (!Objects.equals(newScriptVo.getRiskName(), oldScriptVo.getRiskName())) {
            return true;
        }
        if (!Objects.equals(newScriptVo.getDescription(), oldScriptVo.getDescription())) {
            return true;
        }
        if (!Objects.equals(newScriptVo.getDefaultProfileName(), oldScriptVo.getDefaultProfileName())) {
            return true;
        }
        return false;
    }

    /**
     * 构造AutoexecScriptVersionVo
     *
     * @param scriptVo
     * @param version
     * @return
     */
    private AutoexecScriptVersionVo getVersionVo(AutoexecScriptVo scriptVo, Integer version) {
        AutoexecScriptVersionVo versionVo = new AutoexecScriptVersionVo();
        versionVo.setScriptId(scriptVo.getId());
        versionVo.setTitle(scriptVo.getName());
        versionVo.setParser(scriptVo.getParser());
        versionVo.setIsActive(1);
        versionVo.setLcu(UserContext.get().getUserUuid());
        versionVo.setStatus(ScriptVersionStatus.PASSED.getValue());
        versionVo.setVersion(version);
        versionVo.setReviewer(UserContext.get().getUserUuid());
        AutoexecScriptArgumentVo argument = scriptVo.getVersionArgument();
        if (argument != null) {
            argument.setScriptVersionId(versionVo.getId());
            versionVo.setArgument(argument);
        }
        versionVo.setParamList(scriptVo.getVersionParamList());
        versionVo.setLineList(scriptVo.getLineList());
        return versionVo;
    }

    static List<String> needDataSourceTypeList = new ArrayList<>();

    static {
        needDataSourceTypeList.add(ParamType.SELECT.getValue());
        needDataSourceTypeList.add(ParamType.MULTISELECT.getValue());
        needDataSourceTypeList.add(ParamType.RADIO.getValue());
        needDataSourceTypeList.add(ParamType.CHECKBOX.getValue());
    }
}
