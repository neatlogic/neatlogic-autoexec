package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import neatlogic.framework.autoexec.constvalue.ExecMode;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobOperationAuditTargetVo;
import neatlogic.framework.autoexec.job.source.type.IAutoexecJobSourceTypeHandler;

/** SQL 快照通过来源接口测试，不访问任何数据库或执行器。 */
public class AutoexecJobOperationAuditSnapshotTest {
    /** 全部重置和忽略统一记录阶段快照，不读取阶段下的目标列表。 */
    @Test
    public void resetAndIgnoreAllUsePhaseIdentity() {
        AutoexecJobOperationAuditSnapshotService service = new AutoexecJobOperationAuditSnapshotService();
        JSONObject request = new JSONObject(); request.put("isAll", 1);
        for (String mode : new String[]{ExecMode.SQL.getValue(), "target", ExecMode.RUNNER.getValue()}) {
            AutoexecJobPhaseVo phase = phase(); phase.setExecMode(mode);
            for (JobAction action : new JobAction[]{JobAction.RESET_NODE, JobAction.RESET_SQL, JobAction.IGNORE_NODE}) {
                String type = service.objectType(action, phase, request);
                Assert.assertEquals("phase", type);
                List<AutoexecJobOperationAuditTargetVo> targets = service.targets(job(), phase, type, request);
                Assert.assertEquals(1, targets.size());
                Assert.assertEquals(phase.getId(), targets.get(0).getObjectId());
                Assert.assertEquals(phase.getName(), targets.get(0).getPhaseName());
                Assert.assertNull(targets.get(0).getSqlFile());
            }
        }
    }

    /** 单个及选中批量不升级为阶段，其他全部动作也不受影响。 */
    @Test
    public void selectedTargetsAndOtherActionsKeepTheirType() {
        AutoexecJobOperationAuditSnapshotService service = service(2);
        JSONObject request = new JSONObject();
        for (JobAction action : new JobAction[]{JobAction.RESET_NODE, JobAction.RESET_SQL, JobAction.IGNORE_NODE}) {
            Assert.assertEquals("sql", service.objectType(action, phase(), request));
            request.put("isAll", 0);
            request.put("sqlIdList", new JSONArray(java.util.Arrays.asList(1L, 2L)));
            Assert.assertEquals("sql", service.objectType(action, phase(), request));
            AutoexecJobPhaseVo nodePhase = phase(); nodePhase.setExecMode("target");
            Assert.assertEquals(action.getAuditObjectType(), service.objectType(action, nodePhase, request));
        }
        request.put("isAll", 1);
        Assert.assertEquals("sql", service.objectType(JobAction.REFIRE_NODE, phase(), request));
        Assert.assertEquals("job", service.objectType(JobAction.FIRE, phase(), request));
    }

    /** 同名 SQL 文件按唯一 ID 区分，来源返回的正文和凭据不能进入快照。 */
    @Test
    public void sameFileOnDifferentNodesRetainsIdentityAndDropsSensitiveFields() {
        AutoexecJobOperationAuditSnapshotService service = service(2);
        JSONObject request = new JSONObject(); request.put("sqlIdList", new JSONArray(java.util.Collections.singletonList(2L)));
        List<AutoexecJobOperationAuditTargetVo> targets = service.targets(job(), phase(), "sql", request);
        Assert.assertEquals(1, targets.size()); Assert.assertEquals(Long.valueOf(2), targets.get(0).getObjectId());
        Assert.assertEquals("10.0.0.2", targets.get(0).getHost()); Assert.assertEquals("upgrade.sql", targets.get(0).getSqlFile());
        Assert.assertFalse(JSONObject.toJSONString(targets).contains("secret"));
    }

    /** 全部操作跨页抓取完整集合，不能只记录第一页。 */
    @Test
    public void allSqlTargetsAreReadAcrossPages() {
        JSONObject request = new JSONObject(); request.put("isAll", 1);
        List<AutoexecJobOperationAuditTargetVo> targets = service(201).targets(job(), phase(), "sql", request);
        Assert.assertEquals(201, targets.size()); Assert.assertEquals(Long.valueOf(201), targets.get(200).getObjectId());
    }

    /** 单个 SQL 人工交互按文件与节点定位，避免命中其它同名文件。 */
    @Test
    public void interactionMatchesResourceAndFilename() {
        JSONObject request = new JSONObject(); request.put("resourceId", 2L); request.put("sqlName", "upgrade.sql");
        List<AutoexecJobOperationAuditTargetVo> targets = service(3).targets(job(), phase(), "sql", request);
        Assert.assertEquals(1, targets.size()); Assert.assertEquals(Long.valueOf(2), targets.get(0).getResourceId());
    }

    /** 提供可分页的来源替身，只返回模拟 SQL 元数据。 */
    private AutoexecJobOperationAuditSnapshotService service(int count) {
        IAutoexecJobSourceTypeHandler source = (IAutoexecJobSourceTypeHandler) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IAutoexecJobSourceTypeHandler.class}, (proxy, method, args) -> {
            if (!method.getName().equals("getOperationAuditSqlPage")) { return null; }
            AutoexecJobPhaseNodeVo search = (AutoexecJobPhaseNodeVo) args[0];
            JSONArray rows = new JSONArray();
            for (int id = (search.getCurrentPage() - 1) * 200 + 1; id <= Math.min(count, search.getCurrentPage() * 200); id++) {
                JSONObject row = new JSONObject(); row.put("id", id); row.put("resourceId", id);
                row.put("host", "10.0.0." + id); row.put("sqlFile", "upgrade.sql"); row.put("status", "failed");
                row.put("password", "secret"); row.put("content", "secret SQL正文"); rows.add(row);
            }
            JSONObject result = new JSONObject(); result.put("tbodyList", rows); return result;
        });
        return new AutoexecJobOperationAuditSnapshotService() {
            @Override
            protected IAutoexecJobSourceTypeHandler sqlSource(AutoexecJobVo job) { return source; }
        };
    }
    /** 构造模拟作业。 */
    private AutoexecJobVo job() { AutoexecJobVo job = new AutoexecJobVo(); job.setId(1L); return job; }
    /** 构造模拟 SQL 阶段。 */
    private AutoexecJobPhaseVo phase() { AutoexecJobPhaseVo phase = new AutoexecJobPhaseVo(); phase.setId(3L); phase.setJobId(1L); phase.setName("SQL阶段"); phase.setExecMode(ExecMode.SQL.getValue()); return phase; }
}
