package codedriver.module.autoexec.api.combop;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_MODIFY;
import codedriver.framework.autoexec.dto.combop.*;
import codedriver.framework.autoexec.exception.AutoexecTypeNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.file.FileExtNotAllowedException;
import codedriver.framework.exception.file.FileNotUploadException;
import codedriver.framework.notify.dao.mapper.NotifyMapper;
import codedriver.framework.notify.exception.NotifyPolicyNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.collections4.CollectionUtils;
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
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
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
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecCombopService autoexecCombopService;

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

//    @Input({
//            @Param(name = "fileNameList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "需要导入的文件名列表")
//    })
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
        if (multipartFileMap == null || multipartFileMap.isEmpty()) {
            throw new FileNotUploadException();
        }
        List<String> resultList = new ArrayList<>();
        byte[] buf = new byte[1024];
        //遍历导入文件
        for (Entry<String, MultipartFile> entry : multipartFileMap.entrySet()) {
            MultipartFile multipartFile = entry.getValue();
            //反序列化获取对象
            try (ZipInputStream zipis = new ZipInputStream(multipartFile.getInputStream());
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                while (zipis.getNextEntry() != null) {
                    int len = 0;
                    while ((len = zipis.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    AutoexecCombopVo autoexecCombopVo = JSONObject.parseObject(new String(out.toByteArray(), StandardCharsets.UTF_8), new TypeReference<AutoexecCombopVo>() {
                    });
                    resultList.add(save(autoexecCombopVo));
                    out.reset();
                }
            } catch (IOException e) {
                throw new FileExtNotAllowedException(multipartFile.getOriginalFilename());
            }
        }
        return resultList;
    }

    private String save(AutoexecCombopVo autoexecCombopVo) {
        String result;
        if (autoexecTypeMapper.checkTypeIsExistsById(autoexecCombopVo.getTypeId()) == 0) {
            throw new AutoexecTypeNotFoundException(autoexecCombopVo.getTypeId());
        }
        if (autoexecCombopVo.getNotifyPolicyId() != null) {
            if (notifyMapper.checkNotifyPolicyIsExists(autoexecCombopVo.getNotifyPolicyId()) == 0) {
                throw new NotifyPolicyNotFoundException(autoexecCombopVo.getNotifyPolicyId().toString());
            }
        }
        int index = 0;
        String oldName = autoexecCombopVo.getName();
        //如果导入的流程名称已存在就重命名
        while (autoexecCombopMapper.checkAutoexecCombopNameIsRepeat(autoexecCombopVo) != null) {
            index++;
            autoexecCombopVo.setName(oldName + "_" + index);
        }
        Long id = autoexecCombopVo.getId();
        int isExist = autoexecCombopMapper.checkAutoexecCombopIsExists(id);
        if (isExist == 0) {
            result = "新建组合工具：'" + autoexecCombopVo.getName() + "'";
        } else {
            result = "更新组合工具：'" + autoexecCombopVo.getName() + "'";
            List<Long> combopPhaseIdList = autoexecCombopMapper.getCombopPhaseIdListByCombopId(id);

            if (CollectionUtils.isNotEmpty(combopPhaseIdList)) {
                autoexecCombopMapper.deleteAutoexecCombopPhaseOperationByCombopPhaseIdList(combopPhaseIdList);
            }
            autoexecCombopMapper.deleteAutoexecCombopPhaseByCombopId(id);
            autoexecCombopMapper.deleteAutoexecCombopParamByCombopId(id);
        }
        String userUuid = UserContext.get().getUserUuid(true);
        autoexecCombopVo.setFcu(userUuid);
        autoexecCombopVo.setOwner(userUuid);
        List<AutoexecCombopParamVo> runtimeParamList = autoexecCombopVo.getRuntimeParamList();
        if (CollectionUtils.isNotEmpty(runtimeParamList)) {
            autoexecCombopMapper.insertAutoexecCombopParamVoList(runtimeParamList);
        }
        /** 保存前，校验组合工具是否配置正确，不正确不可以保存 **/
        autoexecCombopService.verifyAutoexecCombopConfig(autoexecCombopVo);
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
                        autoexecCombopMapper.insertAutoexecCombopPhaseOperation(autoexecCombopPhaseOperationVo);
                    }
                }
                autoexecCombopMapper.insertAutoexecCombopPhase(autoexecCombopPhaseVo);
            }
        }
        if (isExist == 0) {
            autoexecCombopMapper.insertAutoexecCombop(autoexecCombopVo);
        } else {
            autoexecCombopMapper.updateAutoexecCombopById(autoexecCombopVo);
        }
        return result;
    }
}
