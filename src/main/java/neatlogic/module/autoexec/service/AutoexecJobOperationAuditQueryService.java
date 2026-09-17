package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dto.job.AutoexecJobOperationAuditVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobOperationAuditTargetVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.autoexec.exception.AutoexecJobOperationAuditNotFoundException;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.util.$;
import neatlogic.module.autoexec.dao.mapper.AutoexecJobOperationAuditMapper;

/** 查询当前租户作业的审计快照，保持与作业详情一致的查看边界。 */
@Service
public class AutoexecJobOperationAuditQueryService {
    @Resource private AutoexecJobMapper jobMapper;
    @Resource private UserMapper userMapper;
    @Resource private AutoexecJobOperationAuditMapper mapper;

    /** 验证作业存在；入口的 AUTOEXEC_BASE 权限与现有作业详情一致。 */
    private void checkJob(Long jobId) {
        if (jobId == null || jobMapper.getJobInfo(jobId) == null) { throw new AutoexecJobNotFoundException(String.valueOf(jobId)); }
    }

    /** 有界分页，失效页码收敛到最后一页。 */
    private JSONObject page(JSONObject request, int count) {
        int size = request.getInteger("pageSize") == null ? 20 : request.getIntValue("pageSize");
        int current = request.getInteger("currentPage") == null ? 1 : request.getIntValue("currentPage");
        if (size < 1 || size > 100 || current < 1) { throw new ParamIrregularException("currentPage >= 1; 1 <= pageSize <= 100"); }
        current = Math.min(current, Math.max(1, (count + size - 1) / size));
        JSONObject result = new JSONObject();
        result.put("currentPage", current); result.put("pageSize", size); result.put("rowNum", count);
        result.put("pageCount", (count + size - 1) / size);
        return result;
    }

    /** 组合条件全部使用参数绑定，关键字同时匹配主对象和目标快照。 */
    public JSONObject search(JSONObject request) {
        checkJob(request.getLong("jobId"));
        JSONObject query = new JSONObject();
        for (String key : new String[]{"jobId", "keyword", "objectType", "action", "operatorUuid", "startTime", "endTime"}) {
            if (request.get(key) != null) { query.put(key, request.get(key)); }
        }
        if (query.getString("action") != null && !query.getString("action").isEmpty()) {
            query.put("actions", JobAction.getAuditQueryValues(query.getString("action")));
        }
        String operator = query.getString("operatorUuid");
        if (operator != null && operator.startsWith("user#")) { query.put("operatorUuid", operator.substring(5)); }
        if (query.getLong("startTime") != null && query.getLong("endTime") != null && query.getLong("startTime") > query.getLong("endTime")) {
            throw new ParamIrregularException("startTime <= endTime");
        }
        JSONObject result = page(request, mapper.countAudits(query));
        query.put("startNum", (result.getIntValue("currentPage") - 1) * result.getIntValue("pageSize"));
        query.put("pageSize", result.getIntValue("pageSize"));
        JSONArray rows = new JSONArray();
        Map<String, String> userNames = new HashMap<>();
        for (AutoexecJobOperationAuditVo audit : mapper.searchAudits(query)) { rows.add(display(audit, userNames)); }
        result.put("tbodyList", rows);
        JSONArray actions = new JSONArray();
        for (JobAction action : JobAction.getAuditActions()) {
            JSONObject option = new JSONObject();
            option.put("value", action.getAuditGroup()); option.put("text", action.getAuditText());
            actions.add(option);
        }
        result.put("actionList", actions);
        return result;
    }

    /** 同时使用 jobId 和记录 ID 读取，目标分页不会跨记录泄露。 */
    public JSONObject get(JSONObject request) {
        Long jobId = request.getLong("jobId");
        checkJob(jobId);
        AutoexecJobOperationAuditVo audit = mapper.getAudit(jobId, request.getLong("id"));
        if (audit == null) { throw new AutoexecJobOperationAuditNotFoundException(jobId, request.getLong("id")); }
        JSONObject result = display(audit, new HashMap<>());
        JSONObject page = page(request, audit.getTargetCount() == null ? 0 : audit.getTargetCount());
        List<AutoexecJobOperationAuditTargetVo> targets = mapper.getTargets(audit.getId(),
                (page.getIntValue("currentPage") - 1) * page.getIntValue("pageSize"), page.getIntValue("pageSize"));
        JSONArray rows = new JSONArray();
        for (AutoexecJobOperationAuditTargetVo target : targets) {
            JSONObject row = JSON.parseObject(JSON.toJSONString(target));
            rows.add(row);
        }
        page.put("tbodyList", rows); result.put("targets", page);
        return result;
    }

    /** 操作编码独立于语言，展示时再进行翻译。 */
    private JSONObject display(AutoexecJobOperationAuditVo audit, Map<String, String> userNames) {
        JSONObject result = JSON.parseObject(JSON.toJSONString(audit));
        String uuid = audit.getOperatorUuid();
        if (uuid != null && !userNames.containsKey(uuid)) {
            UserVo user = userMapper.getUserByUuid(uuid);
            userNames.put(uuid, user == null || user.getUserName() == null ? uuid : user.getUserName());
        }
        result.put("operatorName", userNames.get(uuid));
        result.put("actionText", $.t("autoexec.operationaudit.action." + JobAction.normalizeAuditAction(audit.getAction())));
        result.put("objectTypeText", $.t("autoexec.operationaudit.object." + audit.getObjectType()));
        if (audit.getStrategy() != null) { result.put("strategyText", $.t("autoexec.operationaudit.strategy." + audit.getStrategy())); }
        if ("[REDACTED]".equals(audit.getInteractionValue())) { result.put("interactionValue", $.t("autoexec.operationaudit.redacted")); }
        return result;
    }
}
