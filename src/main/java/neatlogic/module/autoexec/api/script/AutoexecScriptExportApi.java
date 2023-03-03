/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
 */

package neatlogic.module.autoexec.api.script;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.script.*;
import neatlogic.framework.autoexec.exception.AutoexecScriptNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.FileUtil;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptExportApi extends PrivateBinaryStreamApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/export";
    }

    @Override
    public String getName() {
        return "导出脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "idList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "脚本ID列表"),
    })
    @Output({
    })
    @Description(desc = "导出脚本")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<Long> idList = paramObj.getJSONArray("idList").toJavaList(Long.class);
        List<Long> existedIdList = autoexecScriptMapper.checkScriptIdListExists(idList);
        idList.removeAll(existedIdList);
        if (CollectionUtils.isNotEmpty(idList)) {
            throw new AutoexecScriptNotFoundException(StringUtils.join(idList, ","));
        }
        List<AutoexecScriptVo> scriptBaseInfoVoList = autoexecScriptMapper.getAutoexecScriptBaseInfoByIdList(existedIdList);

        Map<Long, AutoexecScriptVersionVo> lineMap = new HashMap<>();
        Map<Long, AutoexecScriptArgumentVo> argumentMap = new HashMap<>();
        Map<Long, List<AutoexecParamVo>> paramMap = new HashMap<>();
        Map<Long, List<Long>> useLibMap = new HashMap<>();
        List<AutoexecScriptVersionVo> versionVoIncludeLineList = autoexecScriptMapper.getActiveVersionListIncludeLineByScriptIdList(existedIdList);
        List<AutoexecScriptVersionVo> versionVoIncludeArgumentList = autoexecScriptMapper.getActiveVersionIncludeArgumentByScriptIdList(existedIdList);
        List<AutoexecScriptVo> scriptVoIncludeParamList = autoexecScriptMapper.getScriptListIncludeActiveVersionParamByScriptIdList(existedIdList);
        List<AutoexecScriptVersionVo> versionVoIncludeUseLibNameList = autoexecScriptMapper.getActiveVersionIncludeUseLibAndNameByScriptIdList(existedIdList);

        if (CollectionUtils.isNotEmpty(versionVoIncludeLineList)) {
            lineMap = versionVoIncludeLineList.stream().collect(Collectors.toMap(AutoexecScriptVersionVo::getScriptId, e -> e));
        }
        if (CollectionUtils.isNotEmpty(versionVoIncludeArgumentList)) {
            argumentMap = versionVoIncludeArgumentList.stream().filter(e -> e.getArgument() != null).collect(Collectors.toMap(AutoexecScriptVersionVo::getScriptId, AutoexecScriptVersionVo::getArgument));
        }
        if (CollectionUtils.isNotEmpty(scriptVoIncludeParamList)) {
            paramMap = scriptVoIncludeParamList.stream().collect(Collectors.toMap(AutoexecScriptVo::getId, AutoexecScriptVo::getParamList));
        }
        if (CollectionUtils.isNotEmpty(versionVoIncludeUseLibNameList)) {
            useLibMap = versionVoIncludeUseLibNameList.stream().collect(Collectors.toMap(AutoexecScriptVersionVo::getScriptId, AutoexecScriptVersionVo::getUseLib));
        }
        String fileName = FileUtil.getEncodedFileName("自定义工具." + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".pak");
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (AutoexecScriptVo scriptVo : scriptBaseInfoVoList) {
                AutoexecScriptVersionVo version = lineMap.get(scriptVo.getId());
                if (version == null) {
                    continue;
                }
                scriptVo.setParser(version.getParser());
                scriptVo.setArgument(argumentMap.get(scriptVo.getId()));
                scriptVo.setParamList(paramMap.get(scriptVo.getId()));
                scriptVo.setLineList(version.getLineList());
                scriptVo.setUseLib(useLibMap.get(scriptVo.getId()));
                zos.putNextEntry(new ZipEntry(scriptVo.getName() + ".json"));
                zos.write(JSONObject.toJSONBytes(scriptVo));
                zos.closeEntry();
            }
        }

//            for (Long id : existedIdList) {
//                AutoexecScriptVo script = autoexecScriptMapper.getScriptBaseInfoById(id);
////                List<AutoexecScriptVersionVo> versionList = autoexecScriptService
////                        .getScriptVersionDetailListByScriptId(new AutoexecScriptVersionVo(id, ScriptVersionStatus.PASSED.getValue()));
////                script.setVersionList(versionList);
//                AutoexecScriptVersionVo version = autoexecScriptMapper.getActiveVersionWithUseLibsByScriptId(id);
//                script.setParser(version.getParser());
//                script.setArgument(autoexecScriptMapper.getArgumentByVersionId(version.getId()));
//                script.setParamList(autoexecScriptMapper.getAutoexecParamVoListByVersionId(version.getId()));
//                script.setLineList(autoexecScriptMapper.getLineListByVersionId(version.getId()));
//                script.setUseLib(version.getUseLib());
//                zos.putNextEntry(new ZipEntry(script.getName() + ".json"));
//                zos.write(JSONObject.toJSONBytes(script));
//                zos.closeEntry();
//            }
//        }
        return null;
    }


}
