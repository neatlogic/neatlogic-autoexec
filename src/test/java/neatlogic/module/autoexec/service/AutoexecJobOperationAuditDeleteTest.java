package neatlogic.module.autoexec.service;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.module.autoexec.dao.mapper.AutoexecJobOperationAuditMapper;

/** 使用隔离替身验证删除顺序和失败传播，不删除真实作业。 */
public class AutoexecJobOperationAuditDeleteTest {
    /** 两次删除均限定同一个作业，先删除明细以保留关联定位。 */
    @Test
    public void deletesTargetsBeforeAudits() throws Exception {
        List<String> calls = new ArrayList<>();
        store(calls, false).deleteByJobId(42L);
        Assert.assertEquals(Arrays.asList("deleteTargetsByJobId", "deleteAuditsByJobId"), calls);
        Transactional transaction = AutoexecJobOperationAuditStore.class.getMethod("deleteByJobId", Long.class).getAnnotation(Transactional.class);
        Assert.assertEquals(Propagation.MANDATORY, transaction.propagation());
        Assert.assertNotNull(AutoexecJobServiceImpl.class.getMethod("deleteJob",
                AutoexecJobVo.class).getAnnotation(Transactional.class));
    }

    /** 明细删除失败必须透传，不能继续删主记录或吞掉异常。 */
    @Test
    public void failureStopsCleanupAndPropagates() throws Exception {
        List<String> calls = new ArrayList<>();
        try {
            store(calls, true).deleteByJobId(42L);
            Assert.fail("删除失败应透传");
        } catch (IllegalStateException expected) {
            Assert.assertEquals("模拟存储失败", expected.getMessage());
        }
        Assert.assertEquals(Arrays.asList("deleteTargetsByJobId"), calls);
    }

    /** 注入存储替身，验证绑定的作业标识。 */
    private AutoexecJobOperationAuditStore store(List<String> calls, boolean fail) throws Exception {
        AutoexecJobOperationAuditMapper mapper = (AutoexecJobOperationAuditMapper) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{AutoexecJobOperationAuditMapper.class}, (proxy, method, args) -> {
                    Assert.assertEquals(42L, args[0]); calls.add(method.getName());
                    if (fail) { throw new IllegalStateException("模拟存储失败"); }
                    return null;
                });
        AutoexecJobOperationAuditStore store = new AutoexecJobOperationAuditStore();
        Field field = AutoexecJobOperationAuditStore.class.getDeclaredField("mapper");
        field.setAccessible(true); field.set(store, mapper);
        return store;
    }
}
