/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_MODIFY;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.file.FileExtNotAllowedException;
import codedriver.framework.exception.file.FileNotUploadException;
import codedriver.framework.notify.dao.mapper.NotifyMapper;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecTypeMapper;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.collections4.CollectionUtils;
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
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.ZipInputStream;

/**
 * 导入组合工具接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@Transactional
@OperationType(type = OperationTypeEnum.CREATE)
@AuthAction(action = AUTOEXEC_COMBOP_MODIFY.class)
public class AutoexecCombopImportApi extends PrivateBinaryStreamApiComponentBase {

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    private NotifyMapper notifyMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/import";
    }

    @Override
    public String getName() {
        return "导入组合工具";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({
            @Param(name = "Return", type = ApiParamType.JSONARRAY, desc = "导入结果")
    })
    @Description(desc = "导入组合工具")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        //获取所有导入文件
        Map<String, MultipartFile> multipartFileMap = multipartRequest.getFileMap();
        //如果没有导入文件, 抛异常
        if (multipartFileMap.isEmpty()) {
            throw new FileNotUploadException();
        }
        int successCount = 0;
        int failureCount = 0;
        List<String> failureReasonList = new ArrayList<>();
        byte[] buf = new byte[1024];
        //遍历导入文件
        for (Entry<String, MultipartFile> entry : multipartFileMap.entrySet()) {
            MultipartFile multipartFile = entry.getValue();
            //反序列化获取对象
            try (ZipInputStream zipis = new ZipInputStream(multipartFile.getInputStream());
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                while (zipis.getNextEntry() != null) {
                    int len;
                    while ((len = zipis.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    AutoexecCombopVo autoexecCombopVo = JSONObject.parseObject(new String(out.toByteArray(), StandardCharsets.UTF_8), new TypeReference<AutoexecCombopVo>() {});
                    String result = save(autoexecCombopVo);
                    if (StringUtils.isNotBlank(result)) {
                        failureCount++;
                        failureReasonList.add(result);
                    } else {
                        successCount++;
                    }
                    out.reset();
                }
            } catch (IOException e) {
                throw new FileExtNotAllowedException(multipartFile.getOriginalFilename());
            }
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("successCount", successCount);
        resultObj.put("failureCount", failureCount);
        resultObj.put("failureReasonList", failureReasonList);
        return resultObj;
    }

    private String save(AutoexecCombopVo autoexecCombopVo) {
        Long id = autoexecCombopVo.getId();
        String oldName = autoexecCombopVo.getName();
        AutoexecCombopVo oldAutoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
        if (oldAutoexecCombopVo != null) {
            if (Objects.equals(oldAutoexecCombopVo.getConfigStr(), autoexecCombopVo.getConfigStr())) {
                List<AutoexecCombopParamVo> autoexecCombopParamVoList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(id);
                if (Objects.equals(JSONObject.toJSONString(autoexecCombopParamVoList), JSONObject.toJSONString(autoexecCombopVo.getRuntimeParamList()))) {
                    return null;
                }
            }
        }

        Set<String> failureReasonSet = new HashSet<>();
        if (autoexecTypeMapper.checkTypeIsExistsById(autoexecCombopVo.getTypeId()) == 0) {
            failureReasonSet.add("添加工具类型：'" + autoexecCombopVo.getTypeId() + "'");
        }
        if (autoexecCombopVo.getNotifyPolicyId() != null) {
            if (notifyMapper.checkNotifyPolicyIsExists(autoexecCombopVo.getNotifyPolicyId()) == 0) {
                failureReasonSet.add("添加通知策略：'" + autoexecCombopVo.getNotifyPolicyId() + "'");
            }
        }
        int index = 0;
        //如果导入的流程名称已存在就重命名
        while (autoexecCombopMapper.checkAutoexecCombopNameIsRepeat(autoexecCombopVo) != null) {
            index++;
            autoexecCombopVo.setName(oldName + "_" + index);
        }
        String userUuid = UserContext.get().getUserUuid(true);
        autoexecCombopVo.setFcu(userUuid);
        List<AutoexecCombopPhaseVo> combopPhaseList2 = new ArrayList<>();
        List<AutoexecCombopPhaseOperationVo> phaseOperationList2 = new ArrayList<>();
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        int iSort = 0;
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
            if (autoexecCombopPhaseVo != null) {
                autoexecCombopPhaseVo.setId(null);
                autoexecCombopPhaseVo.setCombopId(id);
                autoexecCombopPhaseVo.setSort(iSort++);
                AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
                List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
                Long combopPhaseId = autoexecCombopPhaseVo.getId();
                int jSort = 0;
                for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                    if (autoexecCombopPhaseOperationVo != null) {
                        autoexecCombopPhaseOperationVo.setSort(jSort++);
                        autoexecCombopPhaseOperationVo.setCombopPhaseId(combopPhaseId);
                        phaseOperationList2.add(autoexecCombopPhaseOperationVo);
                        if (Objects.equals(autoexecCombopPhaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
                            AutoexecScriptVo autoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoById(autoexecCombopPhaseOperationVo.getOperationId());
                            if (autoexecScriptVo == null) {
                                failureReasonSet.add("添加自定义工具：'" + autoexecCombopPhaseOperationVo.getOperationId() + "'");
                            } else {
                                AutoexecScriptVersionVo autoexecScriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(autoexecScriptVo.getId());
                                if(autoexecScriptVersionVo == null){
                                    failureReasonSet.add("启用自定义工具：'" + autoexecScriptVo.getName() + "'");
                                }
                            }
                        } else {
                            AutoexecToolVo autoexecToolVo = autoexecToolMapper.getToolById(autoexecCombopPhaseOperationVo.getOperationId());
                            if (autoexecToolVo == null) {
                                failureReasonSet.add("添加工具：'" + autoexecCombopPhaseOperationVo.getOperationId() + "'");
                            } else if (Objects.equals(autoexecToolVo.getIsActive(), 0)) {
                                failureReasonSet.add("启用工具：'" + autoexecToolVo.getName() + "'");
                            }
                        }
                    }
                }
                combopPhaseList2.add(autoexecCombopPhaseVo);
            }
        }

        if (CollectionUtils.isEmpty(failureReasonSet)) {
            if (oldAutoexecCombopVo == null) {
                autoexecCombopMapper.insertAutoexecCombop(autoexecCombopVo);
            } else {
                List<Long> combopPhaseIdList = autoexecCombopMapper.getCombopPhaseIdListByCombopId(id);
                if (CollectionUtils.isNotEmpty(combopPhaseIdList)) {
                    autoexecCombopMapper.deleteAutoexecCombopPhaseOperationByCombopPhaseIdList(combopPhaseIdList);
                }
                autoexecCombopMapper.deleteAutoexecCombopPhaseByCombopId(id);
                autoexecCombopMapper.deleteAutoexecCombopParamByCombopId(id);
                autoexecCombopMapper.updateAutoexecCombopById(autoexecCombopVo);
            }
            List<AutoexecCombopParamVo> runtimeParamList = autoexecCombopVo.getRuntimeParamList();
            if (CollectionUtils.isNotEmpty(runtimeParamList)) {
                for(AutoexecCombopParamVo paramVo : runtimeParamList){
                    paramVo.setCombopId(id);
                }
                autoexecCombopMapper.insertAutoexecCombopParamVoList(runtimeParamList);
            }
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList2) {
                autoexecCombopMapper.insertAutoexecCombopPhase(autoexecCombopPhaseVo);
            }
            for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList2) {
                autoexecCombopMapper.insertAutoexecCombopPhaseOperation(autoexecCombopPhaseOperationVo);
            }
            return null;
        } else {
            StringBuilder result = new StringBuilder();
            result.append("导入：'");
            result.append(oldName);
            result.append("'，");
            result.append("失败；请先在系统中：<br>");
            index = 1;
            for (String reason : failureReasonSet) {
                result.append("&nbsp;&nbsp;&nbsp;");
                result.append(index++);
                result.append(".");
                result.append(reason);
                result.append("<br>");
            }
            return result.toString();
        }
    }
}
