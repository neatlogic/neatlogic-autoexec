package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.dto.AutoexecParamVo;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.util.SpringContextUtil;
import neatlogic.module.autoexec.dao.mapper.AutoexecQuickExecutionMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.context.support.StaticMessageSource;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/** 使用真实候选 SQL 验证权限、分页和批量查询复杂度，不访问业务库或 Runner。 */
public class AutoexecQuickExecutionListTest {
  private JdbcDataSource dataSource;
  private SqlSession session;
  private AutoexecQuickExecutionMapper mapper;
  private ListService service;
  private AuthenticationInfoVo auth;
  private Object previousContext;
  private final AtomicInteger searches = new AtomicInteger();
  private final AtomicInteger counts = new AtomicInteger();

  /** 每个用例创建独立内存库和标准消息源，避免依赖外部数据。 */
  @Before
  public void setup() throws Exception {
    Field context = SpringContextUtil.class.getDeclaredField("ctx");
    context.setAccessible(true);
    previousContext = context.get(null);
    StaticMessageSource messages = new StaticMessageSource();
    messages.setUseCodeAsDefaultMessage(true);
    StaticApplicationContext app = new StaticApplicationContext();
    app.getBeanFactory()
        .registerSingleton("messageSourceAccessor", new MessageSourceAccessor(messages));
    new SpringContextUtil().setApplicationContext(app);
    dataSource = new JdbcDataSource();
    dataSource.setURL(
        "jdbc:h2:mem:quick_list_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    sql(
        "CREATE TABLE autoexec_combop(id bigint PRIMARY KEY,name varchar(200),description"
            + " varchar(200),owner varchar(64),is_active int)");
    sql(
        "CREATE TABLE autoexec_combop_version(id bigint PRIMARY KEY,combop_id bigint,name"
            + " varchar(200),is_active int,status varchar(30),config clob)");
    sql(
        "CREATE TABLE autoexec_combop_authority(combop_id bigint,type varchar(20),uuid"
            + " varchar(64),action varchar(20))");
    Configuration configuration =
        new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
    String path = "neatlogic/module/autoexec/dao/mapper/AutoexecQuickExecutionMapper.xml";
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
      new XMLMapperBuilder(in, configuration, path, configuration.getSqlFragments()).parse();
    }
    session = new SqlSessionFactoryBuilder().build(configuration).openSession(true);
    mapper = session.getMapper(AutoexecQuickExecutionMapper.class);
    service = new ListService();
    Field f = AutoexecQuickExecutionServiceImpl.class.getDeclaredField("quickExecutionMapper");
    f.setAccessible(true);
    f.set(
        service,
        Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[] {AutoexecQuickExecutionMapper.class},
            (p, m, a) -> {
              if (m.getName().equals("searchCandidates")) searches.incrementAndGet();
              if (m.getName().equals("countCandidates")) counts.incrementAndGet();
              try {
                return m.invoke(mapper, a);
              } catch (InvocationTargetException ex) {
                throw ex.getCause();
              }
            }));
    auth =
        new AuthenticationInfoVo(
            "user",
            Collections.singletonList("team"),
            Collections.singletonList("role"),
            Collections.emptySet(),
            new JSONObject());
    UserContext user = UserContext.init((UserContext) null);
    user.setUserUuid("user");
    user.setAuthenticationInfoVo(auth);
    UserContext.init(user);
  }

  /** 关闭会话并恢复全局上下文。 */
  @After
  public void cleanup() throws Exception {
    if (session != null) session.close();
    Field context = SpringContextUtil.class.getDeclaredField("ctx");
    context.setAccessible(true);
    context.set(null, previousContext);
    UserContext.init((UserContext) null);
  }

  /** owner 无授权行仍可见，授权类型和动作必须准确，重复授权不重复返回。 */
  @Test
  public void preciseExecuteAuthorityAndActivationAreAppliedInSql() throws Exception {
    for (int id = 1; id <= 9; id++) seed(id, "other", true, true, "passed", true, 1L);
    sql("UPDATE autoexec_combop SET owner='user' WHERE id=1");
    grant(2, "common", "alluser", "execute");
    grant(2, "user", "user", "execute");
    grant(3, "user", "user", "execute");
    grant(4, "team", "team", "execute");
    grant(5, "role", "role", "execute");
    grant(6, "team", "user", "execute");
    grant(6, "user", "user", "view");
    grant(7, "common", "alluser", "execute");
    sql("UPDATE autoexec_combop SET is_active=0 WHERE id=7");
    grant(8, "common", "alluser", "execute");
    sql("UPDATE autoexec_combop_version SET is_active=0 WHERE combop_id=8");
    grant(9, "common", "alluser", "execute");
    sql("UPDATE autoexec_combop_version SET status='draft' WHERE combop_id=9");
    assertEquals(5, mapper.countCandidates(auth, false, null, Collections.emptyList()));
    assertEquals(
        Arrays.asList(5L, 4L, 3L, 2L, 1L),
        ids(mapper.searchCandidates(auth, false, null, Collections.emptyList(), null, 200)));
    assertEquals(6, mapper.countCandidates(auth, true, null, Collections.emptyList()));
    assertEquals(0, mapper.countCandidates(auth, false, "missing", Collections.emptyList()));
    assertEquals(1, mapper.countCandidates(auth, false, "tool-1", Collections.emptyList()));
  }

  /** 501 个候选只发出三次批量读取和一次计数，过滤后分页不出现空页或虚高总数。 */
  @Test
  public void largeListHasBoundedBatchQueriesAndExactEligiblePagination() throws Exception {
    List<String> expected = new ArrayList<>();
    for (int id = 1; id <= 501; id++) {
      seed(id, "user", true, true, "passed", id % 3 != 0, 1L + (id % 5 == 0 ? 1 : 0));
      if (id % 3 != 0 && id % 5 != 0) expected.add(0, String.valueOf(id));
    }
    JSONObject request = new JSONObject();
    request.put("currentPage", 2);
    request.put("pageSize", 20);
    JSONObject response = service.listTools(request);
    assertEquals(expected.size(), response.getIntValue("rowNum"));
    assertEquals((expected.size() + 19) / 20, response.getIntValue("pageCount"));
    assertEquals(expected.subList(20, 40), summaryIds(response));
    assertEquals(3, searches.get());
    assertEquals(1, counts.get());
    assertEquals(1, service.protocolReads);
    for (Object row : response.getJSONArray("tbodyList"))
      assertFalse(((JSONObject) row).containsKey("runtimeParamList"));
  }

  /** 真实编辑器阶段占位和框架压缩配置均可作为候选。 */
  @Test
  public void editorPlaceholdersAndCompressedConfigsRemainVisible() throws Exception {
    seed(1, "user", true, true, "passed", true, 1L);
    seed(2, "user", true, true, "passed", true, 1L);
    seed(3, "user", true, true, "passed", true, 1L);
    try (Connection connection = dataSource.getConnection();
        PreparedStatement read =
            connection.prepareStatement(
                "SELECT config FROM autoexec_combop_version WHERE combop_id=1")) {
      ResultSet result = read.executeQuery();
      result.next();
      JSONObject fixture = JSON.parseObject(result.getString(1));
      fixture
          .getJSONArray("combopPhaseList")
          .getJSONObject(0)
          .getJSONObject("config")
          .put("executeConfig", new JSONObject());
      updateConfig(1, fixture.toJSONString());
      fixture
          .getJSONArray("combopPhaseList")
          .getJSONObject(0)
          .getJSONObject("config")
          .put(
              "executeConfig",
              JSON.parseObject(
                  "{\"isPresetExecuteConfig\":0,\"executeUser\":{\"mappingMode\":\"constant\",\"value\":\"\"},\"executeNodeConfig\":{}}"));
      updateConfig(2, fixture.toJSONString());
      updateConfig(3, "GZIP:" + neatlogic.framework.util.GzipUtil.compress(fixture.toJSONString()));
    }
    JSONObject response = service.listTools(new JSONObject());
    assertEquals(3, response.getIntValue("rowNum"));
    assertEquals(Arrays.asList("3", "2", "1"), summaryIds(response));
    assertEquals(1, searches.get());
    assertEquals(1, service.protocolReads);
  }

  /** 测试只更新隔离内存库中的配置，不访问租户数据。 */
  private void updateConfig(long id, String config) throws Exception {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement update =
            connection.prepareStatement(
                "UPDATE autoexec_combop_version SET config=? WHERE combop_id=?")) {
      update.setString(1, config);
      update.setLong(2, id);
      update.executeUpdate();
    }
  }

  /** 回显只查询请求编号并遵守权限和准入，输出保持选中顺序和去重。 */
  @Test
  public void selectedIdsAreFilteredBeforeLoadingAndKeepInputOrder() throws Exception {
    seed(1, "user", true, true, "passed", true, 1L);
    seed(2, "user", true, true, "passed", true, 1L);
    seed(3, "other", true, true, "passed", true, 1L);
    seed(4, "user", true, true, "passed", false, 1L);
    JSONObject request = new JSONObject();
    request.put("defaultValue", JSONArray.parseArray("[2,3,1,2,4,999]"));
    request.put("keyword", "does-not-match-selected-tools");
    assertEquals(Arrays.asList("2", "1"), summaryIds(service.listTools(request)));
    assertEquals(1, searches.get());
    assertEquals(1, service.protocolReads);
  }

  /** 无效分页在访问任何候选或协议数据前拒绝。 */
  @Test
  public void invalidPageDoesNotReadAnyData() {
    JSONObject request = new JSONObject();
    request.put("pageSize", 101);
    try {
      service.listTools(request);
      fail();
    } catch (neatlogic.framework.exception.type.ParamIrregularException expected) {
    }
    assertEquals(0, counts.get());
    assertEquals(0, searches.get());
    assertEquals(0, service.protocolReads);
  }

  /** 候选为空时不读取协议元数据。 */
  @Test
  public void emptyCandidatesDoNotReadProtocols() {
    assertEquals(0, service.listTools(new JSONObject()).getIntValue("rowNum"));
    assertEquals(0, searches.get());
    assertEquals(0, service.protocolReads);
  }

  /** 最小合法版本配置，受测字段由各用例显式替换。 */
  private void seed(
      long id,
      String owner,
      boolean active,
      boolean versionActive,
      String status,
      boolean valid,
      Long protocol)
      throws Exception {
    JSONObject execute =
        JSON.parseObject(
            "{\"whenToSpecify\":\"runtime\",\"executeUser\":{\"mappingMode\":\"constant\",\"value\":\"root\"}}");
    execute.put("protocolId", protocol);
    String mode = "target";
    if (!valid) mode = "runner";
    JSONObject config = new JSONObject();
    config.put("executeConfig", execute);
    config.put(
        "combopPhaseList",
        JSONArray.parseArray(
            "[{\"execMode\":\""
                + mode
                + "\",\"config\":{\"phaseOperationList\":[{\"operationType\":\"script\"}]}}]"));
    try (Connection c = dataSource.getConnection();
        PreparedStatement tool =
            c.prepareStatement("INSERT INTO autoexec_combop VALUES(?,?,?,?,?)");
        PreparedStatement version =
            c.prepareStatement("INSERT INTO autoexec_combop_version VALUES(?,?,?,?,?,?)")) {
      tool.setLong(1, id);
      tool.setString(2, "tool-" + id);
      tool.setString(3, "description");
      tool.setString(4, owner);
      tool.setBoolean(5, active);
      tool.executeUpdate();
      version.setLong(1, id + 10000);
      version.setLong(2, id);
      version.setString(3, "tool-" + id);
      version.setBoolean(4, versionActive);
      version.setString(5, status);
      version.setString(6, config.toJSONString());
      version.executeUpdate();
    }
  }

  /** 通过参数绑定写入测试授权数据。 */
  private void grant(long id, String type, String uuid, String action) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement p =
            c.prepareStatement("INSERT INTO autoexec_combop_authority VALUES(?,?,?,?)")) {
      p.setLong(1, id);
      p.setString(2, type);
      p.setString(3, uuid);
      p.setString(4, action);
      p.executeUpdate();
    }
  }

  /** 执行测试结构或无敏感值的常量 SQL。 */
  private void sql(String text) throws Exception {
    try (Connection c = dataSource.getConnection();
        Statement s = c.createStatement()) {
      s.execute(text);
    }
  }

  private List<Long> ids(List<AutoexecCombopVersionVo> versions) {
    List<Long> result = new ArrayList<>();
    for (AutoexecCombopVersionVo v : versions) result.add(v.getCombopId());
    return result;
  }

  private List<String> summaryIds(JSONObject response) {
    List<String> result = new ArrayList<>();
    for (Object row : response.getJSONArray("tbodyList"))
      result.add(((JSONObject) row).getString("id"));
    return result;
  }

  /** 只替换外围权限和协议加载，结构校验和真实 SQL 保留。 */
  private static class ListService extends AutoexecQuickExecutionServiceImpl {
    private int protocolReads;

    @Override
    protected void checkBase() {}

    @Override
    protected boolean hasAllToolAuthority() {
      return false;
    }

    @Override
    protected Set<Long> tagentProtocolIds() {
      protocolReads++;
      return Collections.singleton(1L);
    }

    @Override
    public JSONObject describe(Long id) {
      throw new AssertionError("列表不应逐工具读取详情");
    }

    @Override
    protected JSONArray safeRuntimeParams(List<AutoexecParamVo> parameters) {
      throw new AssertionError("列表不应构造运行参数");
    }
  }
}
