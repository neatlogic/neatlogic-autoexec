/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_MODIFY;
import codedriver.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.module.autoexec.service.AutoexecScriptService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptExportForAutoexecApi extends PrivateBinaryStreamApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/export/forautoexec";
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
            @Param(name = "catalogName", type = ApiParamType.STRING, desc = "目录名称（完整路径）"),
            @Param(name = "catalogList", type = ApiParamType.JSONARRAY, desc = "目录名称（完整路径）列表"),
    })
    @Output({
    })
    @Description(desc = "导出脚本(供外部调用)")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Set<Long> catalogIdSet = new HashSet<>();
        String catalogName = paramObj.getString("catalogName");
        JSONArray catalogList = paramObj.getJSONArray("catalogList");
        if (StringUtils.isNotBlank(catalogName)) {
            Long catalogId = autoexecScriptService.getCatalogIdByCatalogPath(catalogName);
            if (catalogId != null) {
                AutoexecCatalogVo catalogVo = autoexecCatalogMapper.getAutoexecCatalogById(catalogId);
                if (catalogVo != null) {
                    catalogIdSet.addAll(autoexecCatalogMapper.getChildrenByLftRht(catalogVo).stream().map(AutoexecCatalogVo::getId).collect(Collectors.toList()));
                }
            }
        } else if (CollectionUtils.isNotEmpty(catalogList)) {
            for (int i = 0; i < catalogList.size(); i++) {
                String catalogPath = catalogList.getString(i);
                if ("/".equals(catalogPath)) {
                    catalogIdSet.clear();
                    break;
                }
                Long catalogId = autoexecScriptService.getCatalogIdByCatalogPath(catalogPath);
                if (catalogId != null) {
                    catalogIdSet.add(catalogId);
                }
            }
            if (catalogIdSet.size() > 0) {
                List<AutoexecCatalogVo> catalogVoList = autoexecCatalogMapper.getCatalogListByIdList(new ArrayList<>(catalogIdSet));
                catalogIdSet.clear();
                if (catalogVoList.size() > 0) {
                    for (AutoexecCatalogVo catalogVo : catalogVoList) {
                        catalogIdSet.addAll(autoexecCatalogMapper.getChildrenByLftRht(catalogVo).stream().map(AutoexecCatalogVo::getId).collect(Collectors.toList()));
                    }
                }
            }
        }
        // 查询有激活版本的脚本
        List<Long> idList = autoexecScriptMapper.getAutoexecScriptIdListWhichHasActiveVersionByCatalogIdList(new ArrayList<>(catalogIdSet));
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
                    script.setArgument(autoexecScriptMapper.getArgumentByVersionId(version.getId()));
                    script.setParamList(autoexecScriptMapper.getAutoexecParamVoListByVersionId(version.getId()));
                    script.setLineList(autoexecScriptMapper.getLineListByVersionId(version.getId()));
                    writer.writeObject(JSONObject.parseObject(JSON.toJSONString(script, SerializerFeature.DisableCircularReferenceDetect)));// 解决json循环引用问题
                }
                writer.endArray();
                writer.flush();
            }
        }
        return null;
    }


}
