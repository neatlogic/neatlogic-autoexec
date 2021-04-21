/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dto.script.AutoexecScriptLineVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.service.AutoexecScriptService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecScriptVersionCopyApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/version/copy";
    }

    @Override
    public String getName() {
        return "复制脚本版本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "versionId", type = ApiParamType.LONG, isRequired = true, desc = "版本ID"),
    })
    @Output({
            @Param(type = ApiParamType.LONG, desc = "复制生成的版本ID"),
    })
    @Description(desc = "复制脚本版本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long versionId = jsonObj.getLong("versionId");
        AutoexecScriptVersionVo source = autoexecScriptService.getScriptVersionDetailByVersionId(versionId);
        Integer maxVersion = autoexecScriptMapper.getMaxVersionByScriptId(source.getScriptId());
        AutoexecScriptVersionVo target = new AutoexecScriptVersionVo();
        target.setScriptId(source.getScriptId());
        target.setVersion(maxVersion != null ? maxVersion + 1 : 0);
        target.setParser(source.getParser());
        target.setStatus(ScriptVersionStatus.DRAFT.getValue());
        target.setIsActive(0);
        target.setLcu(UserContext.get().getUserUuid());
        List<AutoexecScriptVersionParamVo> paramList = source.getParamList();
        List<AutoexecScriptLineVo> lineList = source.getLineList();
        if (CollectionUtils.isNotEmpty(paramList)) {
            paramList.stream().forEach(o -> o.setScriptVersionId(target.getId()));
            autoexecScriptService.batchInsertScriptVersionParamList(paramList, 100);
        }
        if (CollectionUtils.isNotEmpty(lineList)) {
            lineList.stream().forEach(o -> {
                o.setId(null);
                o.setScriptVersionId(target.getId());
            });
            autoexecScriptService.batchInsertScriptLineList(lineList, 100);
        }
        autoexecScriptMapper.insertScriptVersion(target);

        return target.getId();
    }


}
