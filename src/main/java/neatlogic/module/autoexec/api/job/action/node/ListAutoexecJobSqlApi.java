package neatlogic.module.autoexec.api.job.action.node;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecSqlNodeDetailVo;
import neatlogic.framework.cmdb.crossover.IAttrCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverMapper;
import neatlogic.framework.cmdb.crossover.ICiEntityCrossoverService;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.constvalue.JobSourceType;
import neatlogic.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import neatlogic.framework.deploy.dto.sql.DeploySqlNodeDetailVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/4/25 6:33 下午
 */
@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAutoexecJobSqlApi extends PrivateApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "获取作业执行sql文件状态列表";
    }

    @Override
    public String getToken() {
        return "autoexec/job/sql/list";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id"),
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "作业剧本名"),
            @Param(name = "sysId", type = ApiParamType.LONG, desc = "系统id"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id"),
            @Param(name = "version", type = ApiParamType.STRING, desc = "版本"),
            @Param(name = "sqlFiles", type = ApiParamType.JSONARRAY, desc = "sql文件列表"),
            @Param(name = "operType", type = ApiParamType.ENUM, rule = "auto,deploy", isRequired = true, desc = "来源类型")
    })
    @Output({
    })
    @Description(desc = "获取作业执行sql文件状态列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        if (StringUtils.equals(paramObj.getString("operType"), neatlogic.framework.autoexec.constvalue.JobSourceType.AUTOEXEC.getValue())) {
            Long jobId = paramObj.getLong("jobId");
            String phaseName = paramObj.getString("phaseName");

            if (jobId != null && StringUtils.isNotEmpty(phaseName)) {
                List<AutoexecSqlNodeDetailVo> returnSqlList = autoexecJobMapper.getJobSqlDetailListByJobIdAndPhaseName(jobId, phaseName, paramObj.getJSONArray("sqlFiles"));
                //补充访问地址信息
                if (CollectionUtils.isNotEmpty(returnSqlList)) {
                    ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
                    List<CiEntityVo> entityVoList = ciEntityCrossoverMapper.getCiEntityBaseInfoByIdList(returnSqlList.stream().map(AutoexecSqlNodeDetailVo::getResourceId).collect(Collectors.toList()));
                    if (CollectionUtils.isNotEmpty(entityVoList)) {
                        Map<Long, CiEntityVo> ciEntityVoMap = entityVoList.stream().collect(Collectors.toMap(CiEntityVo::getId, e -> e));
                        //循环sql列表，根据sqlVo里面resourceId获取访问地址
                        for (AutoexecSqlNodeDetailVo sqlDetailVo : returnSqlList) {
                            CiEntityVo ciEntity = ciEntityVoMap.get(sqlDetailVo.getResourceId());
                            sqlDetailVo.setServiceAddr(getServeAddr(ciEntity));
                        }
                    }
                }
                return returnSqlList;
            }

        } else if (StringUtils.equals(paramObj.getString("operType"), JobSourceType.DEPLOY.getValue())) {
            IDeploySqlCrossoverMapper iDeploySqlCrossoverMapper = CrossoverServiceFactory.getApi(IDeploySqlCrossoverMapper.class);
            List<DeploySqlNodeDetailVo> sqlDetailVoList = new ArrayList<>();
            sqlDetailVoList.add(paramObj.toJavaObject(DeploySqlNodeDetailVo.class));
            List<DeploySqlNodeDetailVo> returnSqlList = iDeploySqlCrossoverMapper.getDeploySqlDetailList(sqlDetailVoList);
            if (CollectionUtils.isNotEmpty(returnSqlList)) {
                ICiEntityCrossoverMapper ciEntityCrossoverMapper = CrossoverServiceFactory.getApi(ICiEntityCrossoverMapper.class);
                List<CiEntityVo> entityVoList = ciEntityCrossoverMapper.getCiEntityBaseInfoByIdList(returnSqlList.stream().map(DeploySqlNodeDetailVo::getResourceId).collect(Collectors.toList()));
                if (CollectionUtils.isNotEmpty(entityVoList)) {
                    Map<Long, CiEntityVo> ciEntityVoMap = entityVoList.stream().collect(Collectors.toMap(CiEntityVo::getId, e -> e));
                    //循环sql列表，根据sqlVo里面resourceId获取访问地址
                    for (DeploySqlNodeDetailVo sqlDetailVo : returnSqlList) {
                        CiEntityVo ciEntity = ciEntityVoMap.get(sqlDetailVo.getResourceId());
                        if(ciEntity != null) {
                            sqlDetailVo.setServiceAddr(getServeAddr(ciEntity));
                        }
                    }
                }
            }
            return returnSqlList;
        }
        return null;
    }

    /**
     * 查询服务地址
     *
     * @param ciEntityVo 配置项
     * @return 服务地址值
     */
    private String getServeAddr(CiEntityVo ciEntityVo) {

        IAttrCrossoverMapper attrCrossoverMapper = CrossoverServiceFactory.getApi(IAttrCrossoverMapper.class);
        AttrVo attrVo = attrCrossoverMapper.getAttrByCiIdAndName(ciEntityVo.getCiId(), "service_addr");
        ICiEntityCrossoverService ciEntityService = CrossoverServiceFactory.getApi(ICiEntityCrossoverService.class);
        CiEntityVo ciEntityInfo = ciEntityService.getCiEntityById(ciEntityVo.getCiId(), ciEntityVo.getId());
        //根据访问地址的属性id获取对应的值
        if (attrVo != null) {
            JSONObject valueJsonObject = ciEntityInfo.getAttrEntityData().getJSONObject("attr_" + attrVo.getId());
            if (valueJsonObject != null) {
                JSONArray valueList = valueJsonObject.getJSONArray("valueList");
                if (CollectionUtils.isNotEmpty(valueList)) {
                    return String.join(",", valueList.toJavaList(String.class));
                }
            }
        }
        return null;
    }
}
