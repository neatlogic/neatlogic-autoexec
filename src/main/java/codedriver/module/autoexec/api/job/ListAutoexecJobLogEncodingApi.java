/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.job;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobLogEncoding;
import codedriver.framework.dao.mapper.ConfigMapper;
import codedriver.framework.dto.ConfigVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author laiwt
 * @since 2022/6/23 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAutoexecJobLogEncodingApi extends PrivateApiComponentBase {

    final static Logger logger = LoggerFactory.getLogger(ListAutoexecJobLogEncodingApi.class);

    @Resource
    ConfigMapper configMapper;

    @Override
    public String getName() {
        return "获取自动化作业日志字符编码集合";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({})
    @Output({})
    @Description(desc = "获取自动化作业日志字符编码集合")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<String> result = null;
        ConfigVo encodingConfig = configMapper.getConfigByKey("autoexec.job.log.encoding");
        if (encodingConfig != null) {
            String encodingConfigValue = encodingConfig.getValue();
            if (StringUtils.isNotBlank(encodingConfigValue)) {
                try {
                    result = JSONArray.parseArray(encodingConfigValue).toJavaList(String.class);
                } catch (Exception ex) {
                    logger.error("autoexec.job.log.encoding格式非JsonArray");
                }
            }
        }
        if (CollectionUtils.isEmpty(result)) {
            result = new ArrayList<>();
            for (JobLogEncoding encoding : JobLogEncoding.values()) {
                result.add(encoding.getValue());
            }
        }
        return result;
    }

    @Override
    public String getToken() {
        return "autoexec/job/log/encoding/list";
    }
}
