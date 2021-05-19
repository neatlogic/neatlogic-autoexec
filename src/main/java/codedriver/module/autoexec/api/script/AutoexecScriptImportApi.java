/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.exception.file.FileExtNotAllowedException;
import codedriver.framework.exception.file.FileNotUploadException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.service.AutoexecScriptService;
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
import java.util.Objects;
import java.util.zip.ZipInputStream;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecScriptImportApi extends PrivateBinaryStreamApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

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
    })
    @Description(desc = "导入脚本")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        //获取所有导入文件
        Map<String, MultipartFile> multipartFileMap = multipartRequest.getFileMap();
        //如果没有导入文件, 抛异常
        if (multipartFileMap.isEmpty()) {
            throw new FileNotUploadException();
        }
        List<String> resultList = new ArrayList<>();
        byte[] buf = new byte[1024];
        //遍历导入文件
        for (Map.Entry<String, MultipartFile> entry : multipartFileMap.entrySet()) {
            MultipartFile multipartFile = entry.getValue();
            //反序列化获取对象
            try (ZipInputStream zipis = new ZipInputStream(multipartFile.getInputStream());
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                while (zipis.getNextEntry() != null) {
                    int len;
                    while ((len = zipis.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    AutoexecScriptVo scriptVo = JSONObject.parseObject(new String(out.toByteArray(), StandardCharsets.UTF_8), new TypeReference<AutoexecScriptVo>() {
                    });
                    save(scriptVo);
                    out.reset();
                }
            } catch (IOException e) {
                throw new FileExtNotAllowedException(multipartFile.getOriginalFilename());
            }
        }
        return null;
    }

    private void save(AutoexecScriptVo scriptVo) {
        Long id = scriptVo.getId();
        List<AutoexecScriptVersionVo> versionList = scriptVo.getVersionList();
        AutoexecScriptVo oldScriptVo = autoexecScriptMapper.getScriptBaseInfoById(id);
        if (oldScriptVo != null) {
            boolean hasChange = false;
            // todo 重写equals
            if (!Objects.equals(scriptVo.getName(), oldScriptVo.getName())) {
                hasChange = true;
            }
            if (!Objects.equals(scriptVo.getExecMode(), oldScriptVo.getExecMode())) {
                hasChange = true;
            }
            if (!Objects.equals(scriptVo.getRiskId(), oldScriptVo.getRiskVo())) {
                hasChange = true;
            }
            if (!Objects.equals(scriptVo.getTypeId(), oldScriptVo.getTypeId())) {
                hasChange = true;
            }
            if (hasChange) {
                autoexecScriptService.validateScriptBaseInfo(scriptVo);
                autoexecScriptMapper.updateScriptBaseInfo(scriptVo);
            }
            if (CollectionUtils.isNotEmpty(versionList)) {
                for (AutoexecScriptVersionVo versionVo : versionList) {
                    AutoexecScriptVersionVo oldVersion = autoexecScriptService.getScriptVersionDetailByVersionId(versionVo.getId());
                    if (oldVersion != null) {
                        if (autoexecScriptService.checkScriptVersionNeedToUpdate(oldVersion, versionVo)) {
                            autoexecScriptMapper.deleteParamByVersionId(versionVo.getId());
                            autoexecScriptMapper.deleteScriptLineByVersionId(versionVo.getId());
                            autoexecScriptService.saveParamList(versionVo.getId(), oldVersion.getParamList(), versionVo.getParamList());
                            autoexecScriptService.saveLineList(id, versionVo.getId(), versionVo.getLineList());
                            autoexecScriptMapper.updateScriptVersion(versionVo);
                        }
                    } else {
                        autoexecScriptService.saveParamList(versionVo.getId(), null, versionVo.getParamList());
                        autoexecScriptService.saveLineList(id, versionVo.getId(), versionVo.getLineList());
                        autoexecScriptMapper.insertScriptVersion(versionVo);
                    }
                }
            }
        } else {
            autoexecScriptService.validateScriptBaseInfo(scriptVo);
            autoexecScriptMapper.insertScript(scriptVo);
            if (CollectionUtils.isNotEmpty(versionList)) {
                for (AutoexecScriptVersionVo versionVo : versionList) {
                    autoexecScriptService.saveParamList(versionVo.getId(), null, versionVo.getParamList());
                    autoexecScriptService.saveLineList(id, versionVo.getId(), versionVo.getLineList());
                    autoexecScriptMapper.insertScriptVersion(versionVo);
                }
            }
        }
    }


}
