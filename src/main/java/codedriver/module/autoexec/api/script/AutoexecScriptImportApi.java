/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.constvalue.ExecMode;
import codedriver.framework.autoexec.constvalue.ScriptParser;
import codedriver.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecRiskMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.exception.file.FileExtNotAllowedException;
import codedriver.framework.exception.file.FileNotUploadException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.module.autoexec.service.AutoexecScriptService;
import codedriver.module.autoexec.service.AutoexecService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class AutoexecScriptImportApi extends PrivateBinaryStreamApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Resource
    private AutoexecRiskMapper autoexecRiskMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Resource
    private AutoexecService autoexecService;

    @Override
    public String getToken() {
        return "autoexec/script/import";
    }

    @Override
    public String getName() {
        return "导入脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
    })
    @Output({
            @Param(name = "successCount", type = ApiParamType.INTEGER, desc = "导入成功数量"),
            @Param(name = "failureCount", type = ApiParamType.INTEGER, desc = "导入失败数量"),
            @Param(name = "failureReasonList", type = ApiParamType.JSONARRAY, desc = "失败原因")
    })
    @Description(desc = "导入脚本")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject resultObj = new JSONObject();
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> multipartFileMap = multipartRequest.getFileMap();
        if (multipartFileMap.isEmpty()) {
            throw new FileNotUploadException();
        }
        JSONArray resultList = new JSONArray();
        byte[] buf = new byte[1024];
        int successCount = 0;
        int failureCount = 0;
        for (Map.Entry<String, MultipartFile> entry : multipartFileMap.entrySet()) {
            MultipartFile multipartFile = entry.getValue();
            try (ZipInputStream zis = new ZipInputStream(multipartFile.getInputStream());
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                while (zis.getNextEntry() != null) {
                    int len;
                    while ((len = zis.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    AutoexecScriptVo scriptVo = JSONObject.parseObject(new String(out.toByteArray(), StandardCharsets.UTF_8), new TypeReference<AutoexecScriptVo>() {
                    });
                    JSONObject result = save(scriptVo);
                    if (MapUtils.isNotEmpty(result)) {
                        resultList.add(result);
                        failureCount++;
                    } else {
                        successCount++;
                    }
                    out.reset();
                }
            } catch (IOException e) {
                throw new FileExtNotAllowedException(multipartFile.getOriginalFilename());
            }
        }
        resultObj.put("successCount", successCount);
        resultObj.put("failureCount", failureCount);
        if (CollectionUtils.isNotEmpty(resultList)) {
            resultObj.put("failureReasonList", resultList);
        }
        return resultObj;
    }

    private JSONObject save(AutoexecScriptVo scriptVo) {
        List<String> failReasonList = new ArrayList<>();
        Long id = scriptVo.getId();
        String name = scriptVo.getName();
        String catalogName = scriptVo.getCatalogName();
        List<AutoexecScriptVersionVo> versionList = scriptVo.getVersionList();
        AutoexecScriptVo oldScriptVo = autoexecScriptMapper.getScriptBaseInfoById(id);
        int index = 0;
        while (autoexecScriptMapper.checkScriptNameIsExists(scriptVo) > 0) {
            index++;
            scriptVo.setName(name + "_" + index);
        }
        Long typeId = autoexecTypeMapper.getTypeIdByName(scriptVo.getTypeName());
        scriptVo.setTypeId(typeId);
        if (typeId == null) {
            failReasonList.add("不存在的工具类型：" + scriptVo.getTypeName());
        }
        // 根据目录名称匹配，只有当目录存在时才保存目录id，目录名称为空或目录不存在时，不保存目录id
        scriptVo.setCatalogId(AutoexecCatalogVo.ROOT_ID);
        if (StringUtils.isNotBlank(catalogName)) {
            AutoexecCatalogVo catalog = autoexecCatalogMapper.getAutoexecCatalogByName(catalogName);
            if (catalog != null) {
                scriptVo.setCatalogId(catalog.getId());
            }
        }
        Long risk = autoexecRiskMapper.getRiskIdByName(scriptVo.getRiskName());
        scriptVo.setRiskId(risk);
        if (risk == null) {
            failReasonList.add("不存在的操作级别：" + scriptVo.getRiskName());
        }
        if (ExecMode.getExecMode(scriptVo.getExecMode()) == null) {
            failReasonList.add("不存在的执行方式：" + scriptVo.getExecMode());
        }
        if (CollectionUtils.isEmpty(failReasonList)) {
            if (oldScriptVo != null) {
                scriptVo.setLcu(UserContext.get().getUserUuid());
                autoexecScriptMapper.updateScriptBaseInfo(scriptVo);
            } else {
                scriptVo.setFcu(UserContext.get().getUserUuid());
                autoexecScriptMapper.insertScript(scriptVo);
            }
            if (CollectionUtils.isNotEmpty(versionList)) {
                for (AutoexecScriptVersionVo versionVo : versionList) {
                    String versionStr = "版本-" + versionVo.getVersion() + "：";
                    try {
                        if (CollectionUtils.isNotEmpty(versionVo.getParamList())) {
                            autoexecService.validateParamList(versionVo.getParamList());
                        }
                    } catch (ApiRuntimeException ex) {
                        failReasonList.add(versionStr + ex.getMessage());
                        continue;
                    }
                    if (StringUtils.isBlank(versionVo.getParser())) {
                        failReasonList.add(versionStr + "脚本解析器为空");
                        continue;
                    } else if (ScriptParser.getScriptParser(versionVo.getParser()) == null) {
                        failReasonList.add(versionStr + "不存在的脚本解析器[" + versionVo.getParser() + "]");
                        continue;
                    }
                    AutoexecScriptVersionVo oldVersion = autoexecScriptMapper.getVersionByVersionIdForUpdate(versionVo.getId());
                    if (oldVersion != null) {
                        oldVersion.setParamList(autoexecScriptMapper.getParamListByVersionId(versionVo.getId()));
                        oldVersion.setLineList(autoexecScriptMapper.getLineListByVersionId(versionVo.getId()));
                        if (autoexecScriptService.checkScriptVersionNeedToUpdate(oldVersion, versionVo)) {
                            autoexecScriptMapper.deleteParamByVersionId(versionVo.getId());
                            autoexecScriptMapper.deleteScriptLineByVersionId(versionVo.getId());
                            autoexecScriptService.saveParamList(versionVo.getId(), versionVo.getParamList());
                            autoexecScriptService.saveLineList(id, versionVo.getId(), versionVo.getLineList());
                            versionVo.setLcu(UserContext.get().getUserUuid());
                            autoexecScriptMapper.updateScriptVersion(versionVo);
                        }
                    } else {
                        autoexecScriptService.saveParamList(versionVo.getId(), versionVo.getParamList());
                        autoexecScriptService.saveLineList(id, versionVo.getId(), versionVo.getLineList());
                        autoexecScriptMapper.insertScriptVersion(versionVo);
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(failReasonList)) {
            JSONObject result = new JSONObject();
            result.put("item", "导入：" + name + "时出现如下问题：");
            result.put("list", failReasonList);
            return result;
        }
        return null;
    }


}
