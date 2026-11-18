package neatlogic.module.autoexec.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.util.SpringContextUtil;
import neatlogic.module.autoexec.dao.mapper.AutoexecCombopVersionMapper;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** 验证可执行版本查询的权限、激活条件及压缩配置映射。 */
public class AutoexecExecutableVersionMapperTest {
  private JdbcDataSource dataSource;
  private SqlSession session;
  private AutoexecCombopVersionMapper mapper;
  private AuthenticationInfoVo auth;
  private Object previousContext;

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
        "jdbc:h2:mem:executable_version_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
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
    configuration
        .getTypeAliasRegistry()
        .registerAlias("CompressHandler", neatlogic.framework.dao.plugin.CompressHandler.class);
    String path = "neatlogic/module/autoexec/dao/mapper/AutoexecCombopVersionMapper.xml";
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
      new XMLMapperBuilder(in, configuration, path, configuration.getSqlFragments()).parse();
    }
    session = new SqlSessionFactoryBuilder().build(configuration).openSession(true);
    mapper = session.getMapper(AutoexecCombopVersionMapper.class);
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
    assertEquals(5, mapper.countExecutableVersions(auth, false, null, Collections.emptyList()));
    assertEquals(
        Arrays.asList(5L, 4L, 3L, 2L, 1L),
        ids(
            mapper.searchExecutableVersions(
                auth, false, null, Collections.emptyList(), null, 200)));
    try (Connection c = dataSource.getConnection();
        PreparedStatement update =
            c.prepareStatement("UPDATE autoexec_combop_version SET config=? WHERE combop_id=1")) {
      update.setString(
          1, "GZIP:" + neatlogic.framework.util.GzipUtil.compress("{\"runtimeParamList\":[]}"));
      update.executeUpdate();
    }
    assertNotNull(
        mapper
            .searchExecutableVersions(auth, false, null, Collections.singletonList(1L), null, 10)
            .get(0)
            .getConfig()
            .getRuntimeParamList());
    assertEquals(6, mapper.countExecutableVersions(auth, true, null, Collections.emptyList()));
    assertEquals(
        0, mapper.countExecutableVersions(auth, false, "missing", Collections.emptyList()));
    assertEquals(1, mapper.countExecutableVersions(auth, false, "tool-1", Collections.emptyList()));
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
}
