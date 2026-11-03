package neatlogic.module.autoexec.api.job;

import com.alibaba.fastjson.JSONObject;
import com.sun.net.httpserver.HttpServer;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobContentVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.exception.runner.RunnerHttpRequestException;
import neatlogic.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import neatlogic.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import neatlogic.framework.common.config.Config;
import neatlogic.module.autoexec.api.job.action.node.SubmitAutoexecJobPhaseNodeWaitInputApi;
import neatlogic.module.autoexec.job.action.handler.node.AutoexecJobNodeSubmitWaitInputHandler;
import neatlogic.module.autoexec.service.AutoexecJobServiceImpl;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/** 仅在显式指定的隔离 MySQL 8 库中验证实际提交接口的事务生命周期。 */
public class WaitInputMysqlIT {
    /** 在网络等待期间验证同行排斥，HTTP 失败后验证 Spring 回滚并释放该行。 */
    @Test(timeout = 30000) public void submitTimeoutReleasesJobLock() throws Exception {
        String url = System.getenv("WAITINPUT_MYSQL_TEST_URL");
        Assume.assumeTrue("需要隔离 MySQL 8", url != null);
        assertTrue(url.matches("jdbc:mysql://[^/]+/waitinput_it_[a-zA-Z0-9_]+(\\?.*)?"));
        DriverManagerDataSource ds = new DriverManagerDataSource(url,
                System.getenv("WAITINPUT_MYSQL_TEST_USER"), System.getenv("WAITINPUT_MYSQL_TEST_PASSWORD"));
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        String table = "waitinput_probe_" + UUID.randomUUID().toString().replace("-", "");
        String lock = "SELECT id FROM " + table + " WHERE id = 42 FOR UPDATE";
        HttpServer runner = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CountDownLatch entered = new CountDownLatch(1);
        CompletableFuture<Void> release = new CompletableFuture<>();
        runner.createContext("/api/rest/job/phase/node/status/get", exchange -> {
            byte[] body = "{\"Status\":\"OK\",\"Return\":{\"interact\":{\"pipeFile\":\"/test/input.pipe\"}}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        runner.createContext("/api/rest/job/phase/node/submit/waitInput", exchange -> {
            try {
                exchange.getRequestBody().readAllBytes();
                entered.countDown();
                release.join();
            } finally {
                exchange.close();
            }
        });

        AutoexecJobMapper mapper = (AutoexecJobMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{AutoexecJobMapper.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getJobLockByJobId":
                            // 保留生产 Mapper 的按主键 FOR UPDATE 语义，仅使用本测试建的表。
                            jdbc.queryForObject(lock, Long.class);
                            jdbc.update("UPDATE " + table + " SET marker=1 WHERE id=42");
                            return new AutoexecJobVo(42L, "waitInput");
                        case "getJobInfo":
                            return new AutoexecJobVo(42L, "waitInput");
                        case "getJobContent":
                            return new AutoexecJobContentVo("test", "{}");
                        default:
                            throw new AssertionError(method);
                    }
                });
        AutoexecJobServiceImpl service = new AutoexecJobServiceImpl();
        field(AutoexecJobServiceImpl.class, "autoexecJobMapper").set(service, mapper);
        AutoexecJobNodeSubmitWaitInputHandler handler = new AutoexecJobNodeSubmitWaitInputHandler() {
            /** 使用隔离节点替代权限与作业快照数据，其余处理走生产逻辑。 */
            @Override public boolean validate(AutoexecJobVo job) {
                AutoexecJobPhaseNodeVo node = new AutoexecJobPhaseNodeVo();
                node.setId(17L);
                node.setJobId(42L);
                node.setRunnerUrl("http://127.0.0.1:" + runner.getAddress().getPort());
                job.setCurrentNode(node);
                job.setExecutePhase(new AutoexecJobPhaseVo());
                return true;
            }
        };
        field(AutoexecJobNodeSubmitWaitInputHandler.class, "autoexecJobService").set(handler, service);
        SubmitAutoexecJobPhaseNodeWaitInputApi target = new SubmitAutoexecJobPhaseNodeWaitInputApi();
        field(SubmitAutoexecJobPhaseNodeWaitInputApi.class, "autoexecJobMapper").set(target, mapper);
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(ds), new AnnotationTransactionAttributeSource()));
        SubmitAutoexecJobPhaseNodeWaitInputApi api = (SubmitAutoexecJobPhaseNodeWaitInputApi) factory.getProxy();
        Field timeout = field(Config.class, "RUNNER_READ_TIMEOUT");
        Object previousTimeout = timeout.get(null);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        jdbc.execute("CREATE TABLE " + table + " (id BIGINT PRIMARY KEY, marker INT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
        IAutoexecJobActionHandler original = AutoexecJobActionHandlerFactory.getActionMap().put(handler.getName(), handler);
        try {
            jdbc.update("INSERT INTO " + table + " VALUES (42, 0)");
            timeout.set(null, 2000);
            runner.start();
            Future<Object> result = worker.submit(() -> {
                UserContext user = UserContext.init((UserContext) null);
                TenantContext.init("test");
                user.setUserUuid("test-user");
                user.setToken("Bearer_fixture");
                try {
                    JSONObject params = new JSONObject();
                    params.put("jobId", 42L);
                    params.put("option", "Yes");
                    return api.myDoService(params);
                } finally {
                    user.release();
                    TenantContext.get().release();
                }
            });
            if (!entered.await(8, TimeUnit.SECONDS)) {
                result.get(1, TimeUnit.SECONDS);
                fail("提交未到达模拟 Runner");
            }
            try (Connection connection = ds.getConnection(); Statement statement = connection.createStatement()) {
                connection.setAutoCommit(false);
                SQLException blocked = assertThrows(SQLException.class, () -> statement.executeQuery(lock + " NOWAIT"));
                assertEquals(3572, blocked.getErrorCode());
                connection.rollback();
                assertTrue(assertThrows(ExecutionException.class, () -> result.get(6, TimeUnit.SECONDS))
                        .getCause() instanceof RunnerHttpRequestException);
                assertTrue(statement.executeQuery(lock + " NOWAIT").next());
                connection.rollback();
            }
            assertEquals(Integer.valueOf(0), jdbc.queryForObject("SELECT marker FROM " + table, Integer.class));
        } finally {
            release.complete(null);
            runner.stop(0);
            worker.shutdownNow();
            timeout.set(null, previousTimeout);
            if (original == null) AutoexecJobActionHandlerFactory.getActionMap().remove(handler.getName());
            else AutoexecJobActionHandlerFactory.getActionMap().put(handler.getName(), original);
            jdbc.execute("DROP TABLE " + table);
        }
    }

    /** 为测试装配局部依赖，避免依赖真实租户和作业。 */
    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
