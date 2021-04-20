/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
import codedriver.framework.autoexec.dto.script.AutoexecScriptLineVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.autoexec.exception.AutoexecScriptNameOrUkRepeatException;
import codedriver.framework.autoexec.exception.AutoexecScriptNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.service.AutoexecScriptService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class AutoexecScriptCopyApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Override
    public String getToken() {
        return "autoexec/script/copy";
    }

    @Override
    public String getName() {
        return "复制脚本";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "脚本ID"),
            @Param(name = "uk", type = ApiParamType.REGEX, rule = "^[A-Za-z]+$", isRequired = true, xss = true, desc = "唯一标识"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = "^[A-Za-z_\\d\\u4e00-\\u9fa5]+$", isRequired = true, xss = true, desc = "名称"),
    })
    @Output({
    })
    @Description(desc = "复制脚本")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        /**
         * 复制所有版本，版本状态也一并复制
         */
        JSONObject result = new JSONObject();
        Long id = jsonObj.getLong("id");
        String uk = jsonObj.getString("uk");
        String name = jsonObj.getString("name");
        AutoexecScriptVo sourceScript = autoexecScriptMapper.getScriptBaseInfoById(id);
        if (sourceScript == null) {
            throw new AutoexecScriptNotFoundException(id);
        }
        AutoexecScriptVo targetScript = new AutoexecScriptVo();
        targetScript.setUk(uk);
        targetScript.setName(name);
        if (autoexecScriptMapper.checkScriptNameIsExists(targetScript) > 0) {
            throw new AutoexecScriptNameOrUkRepeatException(targetScript.getName());
        }
        if (autoexecScriptMapper.checkScriptUkIsExists(targetScript) > 0) {
            throw new AutoexecScriptNameOrUkRepeatException(targetScript.getName());
        }
        targetScript.setTypeId(sourceScript.getTypeId());
        targetScript.setRiskId(sourceScript.getRiskId());
        targetScript.setExecMode(sourceScript.getExecMode());
        targetScript.setFcu(UserContext.get().getUserUuid());
        autoexecScriptMapper.insertScript(targetScript);

        List<AutoexecScriptVersionVo> sourceVersionList = autoexecScriptService.getScriptVersionDetailListByScriptId(sourceScript.getId());
        if (CollectionUtils.isNotEmpty(sourceVersionList)) {
            List<AutoexecScriptVersionVo> targetVersionList = new ArrayList<>();
            List<AutoexecScriptVersionParamVo> paramList = new ArrayList<>();
            List<AutoexecScriptLineVo> lineList = new ArrayList<>();
            for (AutoexecScriptVersionVo source : sourceVersionList) {
                AutoexecScriptVersionVo target = new AutoexecScriptVersionVo();
                BeanUtils.copyProperties(source, target);
                target.setId(null);
                target.setScriptId(targetScript.getId());
                targetVersionList.add(target);
                if (CollectionUtils.isNotEmpty(source.getParamList())) {
                    source.getParamList().stream().forEach(o -> o.setScriptVersionId(target.getId()));
                    paramList.addAll(source.getParamList());
                }
                if (CollectionUtils.isNotEmpty(source.getLineList())) {
                    source.getLineList().stream().forEach(o -> {
                        o.setScriptId(targetScript.getId());
                        o.setScriptVersionId(target.getId());
                    });
                    lineList.addAll(source.getLineList());
                }
            }
            autoexecScriptMapper.batchInsertScriptVersion(targetVersionList);
            if (CollectionUtils.isNotEmpty(paramList)) {
                int count = paramList.size() / 100;
                for (int i = 0; i < count; i++) {
                    autoexecScriptMapper.insertScriptVersionParamList(paramList.subList(i * count, i * count + 100));
                }
            }
            if (CollectionUtils.isNotEmpty(lineList)) {
                int count = lineList.size() / 100;
                for (int i = 0; i < count; i++) {
                    autoexecScriptMapper.insertScriptLineList(lineList.subList(i * count, i * count + 100));
                }
            }
        }


        return result;
    }


}
