/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.file.FileExtNotAllowedException;
import codedriver.framework.exception.file.FileNotUploadException;
import codedriver.framework.notify.dao.mapper.NotifyMapper;
import codedriver.framework.notify.dto.NotifyPolicyVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import com.alibaba.fastjson.JSONArray;
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
@AuthAction(action = AUTOEXEC_COMBOP_ADD.class)
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
        JSONArray failureReasonList = new JSONArray();
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
                    JSONObject resultObj = save(autoexecCombopVo);
                    if (resultObj != null) {
                        failureCount++;
                        failureReasonList.add(resultObj);
                    } else {
                        successCount++;
                    }
                    out.reset();
                }
            } catch (Exception e) {
                throw new FileExtNotAllowedException(multipartFile.getOriginalFilename());
            }
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("successCount", successCount);
        resultObj.put("failureCount", failureCount);
        resultObj.put("failureReasonList", failureReasonList);
        return resultObj;
    }

    private JSONObject save(AutoexecCombopVo autoexecCombopVo) {
        Long id = autoexecCombopVo.getId();
        String oldName = autoexecCombopVo.getName();
        if (StringUtils.isBlank(oldName)) {
            throw new ClassCastException();
        }
        if (autoexecCombopVo.getTypeId() == null){
            throw new ClassCastException();
        }
        if (autoexecCombopVo.getIsActive() == null){
            throw new ClassCastException();
        }
        if (StringUtils.isBlank(autoexecCombopVo.getOperationType())){
            throw new ClassCastException();
        }
        if (StringUtils.isBlank(autoexecCombopVo.getOwner())){
            throw new ClassCastException();
        }
        if (autoexecCombopVo.getConfig() == null){
            throw new ClassCastException();
        }
        AutoexecCombopVo oldAutoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
        if (oldAutoexecCombopVo != null) {
            if (equals(oldAutoexecCombopVo, autoexecCombopVo)) {
                List<AutoexecParamVo> autoexecParamVoList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(id);
                if (Objects.equals(JSONObject.toJSONString(autoexecParamVoList), JSONObject.toJSONString(autoexecCombopVo.getRuntimeParamList()))) {
                    return null;
                }
            }
        }

        Set<String> failureReasonSet = new HashSet<>();
        Long typeId = autoexecTypeMapper.getTypeIdByName(autoexecCombopVo.getTypeName());
        if (typeId == null) {
            failureReasonSet.add("添加工具类型：'" + autoexecCombopVo.getTypeName() + "'");
        } else {
            autoexecCombopVo.setTypeId(typeId);
        }
        if (autoexecCombopVo.getNotifyPolicyName() != null) {
            NotifyPolicyVo notifyPolicyVo = notifyMapper.getNotifyPolicyByName(autoexecCombopVo.getNotifyPolicyName());
            if (notifyPolicyVo == null) {
                failureReasonSet.add("添加通知策略：'" + autoexecCombopVo.getNotifyPolicyName() + "'");
            } else {
                autoexecCombopVo.setNotifyPolicyId(notifyPolicyVo.getId());
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
        AutoexecCombopConfigVo config = autoexecCombopVo.getConfig();
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                if (autoexecCombopPhaseVo != null) {
                    autoexecCombopPhaseVo.setId(null);
                    AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
                    List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
                    if (CollectionUtils.isNotEmpty(phaseOperationList)) {
                        for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                            if (autoexecCombopPhaseOperationVo != null) {
                                if (Objects.equals(autoexecCombopPhaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
                                    AutoexecScriptVo autoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoByName(autoexecCombopPhaseOperationVo.getOperationName());
                                    if (autoexecScriptVo == null) {
                                        failureReasonSet.add("添加自定义工具：'" + autoexecCombopPhaseOperationVo.getOperationName() + "'");
                                    } else {
                                        AutoexecScriptVersionVo autoexecScriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(autoexecScriptVo.getId());
                                        if (autoexecScriptVersionVo == null) {
                                            failureReasonSet.add("启用自定义工具：'" + autoexecScriptVo.getName() + "'");
                                        }
                                        autoexecCombopPhaseOperationVo.setOperationId(autoexecScriptVo.getId());
                                    }
                                } else {
                                    AutoexecToolVo autoexecToolVo = autoexecToolMapper.getToolByName(autoexecCombopPhaseOperationVo.getOperationName());
                                    if (autoexecToolVo == null) {
                                        failureReasonSet.add("添加工具：'" + autoexecCombopPhaseOperationVo.getOperationName() + "'");
                                    } else if (Objects.equals(autoexecToolVo.getIsActive(), 0)) {
                                        failureReasonSet.add("启用工具：'" + autoexecToolVo.getName() + "'");
                                    } else {
                                        autoexecCombopPhaseOperationVo.setOperationId(autoexecToolVo.getId());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isEmpty(failureReasonSet)) {
            autoexecCombopVo.setConfigStr(null);
            if (oldAutoexecCombopVo == null) {
                autoexecCombopMapper.insertAutoexecCombop(autoexecCombopVo);
            } else {
                autoexecCombopMapper.deleteAutoexecCombopParamByCombopId(id);
                autoexecCombopMapper.updateAutoexecCombopById(autoexecCombopVo);
            }
            List<AutoexecParamVo> runtimeParamList = autoexecCombopVo.getRuntimeParamList();
            if (CollectionUtils.isNotEmpty(runtimeParamList)) {
                List<AutoexecCombopParamVo> autoexecCombopParamList = new ArrayList<>();
                for(AutoexecParamVo paramVo : runtimeParamList){
                    AutoexecCombopParamVo autoexecCombopParamVo = new AutoexecCombopParamVo(paramVo);
                    autoexecCombopParamVo.setCombopId(id);
                    autoexecCombopParamList.add(autoexecCombopParamVo);
                }
                autoexecCombopMapper.insertAutoexecCombopParamVoList(autoexecCombopParamList);
            }
            return null;
        } else {
            JSONObject resultObj = new JSONObject();
            resultObj.put("item", "导入：'" + oldName + "'，失败；请先在系统中：");
            resultObj.put("list", failureReasonSet);
            return resultObj;
        }
    }

    private boolean equals(AutoexecCombopVo obj1, AutoexecCombopVo obj2){
        if (!Objects.equals(obj1.getName(), obj2.getName())) {
            return false;
        }
        if (!Objects.equals(obj1.getDescription(), obj2.getDescription())) {
            return false;
        }
        if (!Objects.equals(obj1.getTypeId(), obj2.getTypeId())) {
            return false;
        }
        if (!Objects.equals(obj1.getIsActive(), obj2.getIsActive())) {
            return false;
        }
        if (!Objects.equals(obj1.getOperationType(), obj2.getOperationType())) {
            return false;
        }
        if (!Objects.equals(obj1.getNotifyPolicyId(), obj2.getNotifyPolicyId())) {
            return false;
        }
        if (!Objects.equals(obj1.getOwner(), obj2.getOwner())) {
            return false;
        }
        if (!Objects.equals(obj1.getConfigStr(), obj2.getConfigStr())) {
            return false;
        }
        return true;
    }
}
