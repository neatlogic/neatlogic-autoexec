/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.autoexec.api.combop;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import neatlogic.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import neatlogic.framework.autoexec.dto.AutoexecTypeVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVo;
import neatlogic.framework.autoexec.exception.AutoexecCombopNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.notify.dao.mapper.NotifyMapper;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.FileUtil;
import neatlogic.framework.autoexec.dao.mapper.AutoexecCombopMapper;
import com.alibaba.fastjson.JSONObject;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
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
    private AutoexecCombopVersionMapper autoexecCombopVersionMapper;
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
            //System.out.println(capacity);
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
        List<AutoexecCombopVo> autoexecCombopVoList = new ArrayList<>();
        for (Long id : existIdList) {
            AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
            List<AutoexecCombopVersionVo> versionList = autoexecCombopVersionMapper.getAutoexecCombopVersionListByCombopId(id);
            autoexecCombopVo.setVersionList(versionList);
            typeIdSet.add(autoexecCombopVo.getTypeId());
            autoexecCombopVoList.add(autoexecCombopVo);
        }
        List<AutoexecTypeVo> autoexecTypeList = autoexecTypeMapper.getTypeListByIdList(new ArrayList<>(typeIdSet));
        Map<Long, AutoexecTypeVo> autoexecTypeMap = autoexecTypeList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        //设置导出文件名
        String fileName = FileUtil.getEncodedFileName("组合工具." + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".pak");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");

        try (ZipOutputStream zipos = new ZipOutputStream(response.getOutputStream())) {
            for (AutoexecCombopVo autoexecCombopVo : autoexecCombopVoList) {
                AutoexecTypeVo autoexecTypeVo = autoexecTypeMap.get(autoexecCombopVo.getTypeId());
                if (autoexecTypeVo != null) {
                    autoexecCombopVo.setTypeName(autoexecTypeVo.getName());
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
