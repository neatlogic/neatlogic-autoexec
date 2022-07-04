/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.AutoexecTypeVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopParamVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.notify.dao.mapper.NotifyMapper;
import codedriver.framework.notify.dto.NotifyPolicyVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.FileUtil;
import codedriver.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 导出组合工具接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@AuthAction(action = AUTOEXEC_COMBOP_ADD.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopExportApi extends PrivateBinaryStreamApiComponentBase {

    private final static Logger logger = LoggerFactory.getLogger(AutoexecCombopExportApi.class);

    @Resource
    private AutoexecCombopMapper autoexecCombopMapper;
    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;
    @Resource
    private NotifyMapper notifyMapper;

    @Override
    public String getToken() {
        return "autoexec/combop/export";
    }

    @Override
    public String getName() {
        return "导出组合工具";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "idList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "组合工具id列表")
    })
    @Description(desc = "导出组合工具")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<Long> idList = paramObj.getJSONArray("idList").toJavaList(Long.class);
        if (CollectionUtils.isEmpty(idList)) {
            throw new ParamNotExistsException("idList");
        }
        List<Long> existIdList = autoexecCombopMapper.checkAutoexecCombopIdListIsExists(idList);
        idList.removeAll(existIdList);
        if (CollectionUtils.isNotEmpty(idList)) {
            int capacity = 17 * idList.size();
            System.out.println(capacity);
            StringBuilder stringBuilder = new StringBuilder(capacity);
            for (Long id : idList) {
                stringBuilder.append(id);
                stringBuilder.append("、");
            }
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            System.out.println(stringBuilder.length());
            throw new AutoexecCombopNotFoundException(stringBuilder.toString());
        }
        Set<Long> typeIdSet = new HashSet<>();
        Set<Long> notifyPolicyIdSet = new HashSet<>();
        List<AutoexecCombopVo> autoexecCombopVoList = new ArrayList<>();
        for (Long id : existIdList) {
            AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
            List<AutoexecCombopParamVo> runtimeParamList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(id);
            autoexecCombopVo.setRuntimeParamList(runtimeParamList);
            typeIdSet.add(autoexecCombopVo.getTypeId());
            Long notifyPolicyId = autoexecCombopVo.getNotifyPolicyId();
            if (notifyPolicyId != null) {
                notifyPolicyIdSet.add(notifyPolicyId);
            }
            autoexecCombopVoList.add(autoexecCombopVo);
        }
        List<AutoexecTypeVo> autoexecTypeList = autoexecTypeMapper.getTypeListByIdList(new ArrayList<>(typeIdSet));
        Map<Long, AutoexecTypeVo> autoexecTypeMap = autoexecTypeList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        Map<Long, NotifyPolicyVo> notifyPolicyMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(notifyPolicyIdSet)) {
            List<NotifyPolicyVo> notifyPolicyList = notifyMapper.getNotifyPolicyListByIdList(new ArrayList<>(notifyPolicyIdSet));
            notifyPolicyMap = notifyPolicyList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        }
        //设置导出文件名
        String fileName = FileUtil.getEncodedFileName(request.getHeader("User-Agent"), "组合工具." + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".pak");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");

        try (ZipOutputStream zipos = new ZipOutputStream(response.getOutputStream())) {
            for (AutoexecCombopVo autoexecCombopVo : autoexecCombopVoList) {
                AutoexecTypeVo autoexecTypeVo = autoexecTypeMap.get(autoexecCombopVo.getTypeId());
                if (autoexecTypeVo != null) {
                    autoexecCombopVo.setTypeName(autoexecTypeVo.getName());
                }
                Long notifyPolicyId = autoexecCombopVo.getNotifyPolicyId();
                if (notifyPolicyId != null) {
                    NotifyPolicyVo notifyPolicyVo = notifyPolicyMap.get(notifyPolicyId);
                    if (notifyPolicyVo != null) {
                        autoexecCombopVo.setNotifyPolicyName(notifyPolicyVo.getName());
                    }
                }
                zipos.putNextEntry(new ZipEntry(autoexecCombopVo.getName() + ".json"));
                zipos.write(JSONObject.toJSONBytes(autoexecCombopVo));
                zipos.closeEntry();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
