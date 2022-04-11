/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecCatalogNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicBinaryStreamApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONWriter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptExportPublicApi extends PublicBinaryStreamApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Override
    public String getToken() {
        return "autoexec/script/export";
    }

    @Override
    public String getName() {
        return "导出脚本(供外部调用)";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "catalogName", type = ApiParamType.STRING, desc = "目录名称"),
    })
    @Output({
    })
    @Description(desc = "导出脚本(供外部调用)")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String catalogName = paramObj.getString("catalogName");
        List<Long> catalogIdList = null;
        if (StringUtils.isNotBlank(catalogName)) {
            AutoexecCatalogVo catalog = autoexecCatalogMapper.getAutoexecCatalogByName(catalogName);
            if (catalog == null) {
                throw new AutoexecCatalogNotFoundException(catalogName);
            }
            catalogIdList = autoexecCatalogMapper.getChildrenIdListByLeftRightCode(catalog.getLft(), catalog.getRht());
            catalogIdList.add(catalog.getId());
        }
        // 查询有激活版本的脚本
        List<Long> idList = autoexecScriptMapper.getAutoexecScriptIdListWhichHasActiveVersionByCatalogIdList(catalogIdList);
        if (!idList.isEmpty()) {
            try (JSONWriter writer = new JSONWriter(response.getWriter())) {
                writer.startArray();
                for (Long id : idList) {
                    AutoexecScriptVo script = autoexecScriptMapper.getScriptBaseInfoById(id);
                    AutoexecCatalogVo _catalog = autoexecCatalogMapper.getAutoexecCatalogById(script.getCatalogId());
                    if (_catalog != null) {
                        List<AutoexecCatalogVo> upwardList = autoexecCatalogMapper.getAncestorsAndSelfByLR(_catalog.getLft(), _catalog.getRht());
                        script.setCatalogPath(upwardList.stream().map(AutoexecCatalogVo::getName).collect(Collectors.joining("/")));
                    }
                    AutoexecScriptVersionVo version = autoexecScriptMapper.getActiveVersionByScriptId(id);
                    script.setParser(version.getParser());
                    script.setParamList(autoexecScriptMapper.getAutoexecParamVoListByVersionId(version.getId()));
                    script.setLineList(autoexecScriptMapper.getLineListByVersionId(version.getId()));
                    writer.writeObject(script);
                }
                writer.endArray();
                writer.flush();
            }
        }
        return null;
    }


}
