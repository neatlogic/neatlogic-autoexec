/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_ADD;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.file.FileExtNotAllowedException;
import codedriver.framework.exception.file.FileNotUploadException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
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
@AuthAction(action = AUTOEXEC_COMBOP_ADD.class)
public class AutoexecCombopImportListApi extends PrivateBinaryStreamApiComponentBase {

    @Override
    public String getToken() {
        return "autoexec/combop/import/list";
    }

    @Override
    public String getName() {
        return "导入组合工具列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, desc = "组合工具列表")
    })
    @Description(desc = "导入组合工具列表")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        //获取所有导入文件
        Map<String, MultipartFile> multipartFileMap = multipartRequest.getFileMap();
        //如果没有导入文件, 抛异常
        if (multipartFileMap.isEmpty()) {
            throw new FileNotUploadException();
        }

        List<String> tbodyList = new ArrayList<>();
        //遍历导入文件
        for (Entry<String, MultipartFile> entry : multipartFileMap.entrySet()) {
            MultipartFile multipartFile = entry.getValue();
            //反序列化获取对象
            try (ZipInputStream zipis = new ZipInputStream(multipartFile.getInputStream())) {
                ZipEntry zipEntry = null;
                while ((zipEntry = zipis.getNextEntry()) != null) {
                    String name = zipEntry.getName();
                    if (StringUtils.isBlank(name)) {
                        continue;
                    }
                    if (name.endsWith(".json")) {
                        name = name.substring(0, name.length() - 5);
                    }
                    tbodyList.add(name);
                }
            } catch (Exception e) {
                throw new FileExtNotAllowedException(multipartFile.getOriginalFilename());
            }
        }
        return TableResultUtil.getResult(tbodyList);
    }

}
