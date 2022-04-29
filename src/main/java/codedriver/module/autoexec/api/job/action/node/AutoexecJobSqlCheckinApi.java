package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.autoexec.constvalue.AutoexecOperType;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobSqlDetailVo;
import codedriver.framework.deploy.dto.sql.DeployJobSqlVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.DeployOperType;
import codedriver.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import codedriver.framework.deploy.dto.sql.DeploySqlDetailVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/4/25 5:46 下午
 */

@Service
@Transactional
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobSqlCheckinApi extends PublicApiComponentBase {

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return "检查作业执行sql文件状态";
    }

    @Override
    public String getToken() {
        return "autoexec/job/sql/checkin";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "sqlVoList", type = ApiParamType.JSONARRAY, desc = "sql文件列表"),
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id"),
            @Param(name = "systemId", type = ApiParamType.LONG, desc = "系统id"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id"),
            @Param(name = "version", type = ApiParamType.LONG, desc = "版本"),
            @Param(name = "operType", type = ApiParamType.STRING, isRequired = true, desc = "标记")
    })
    @Output({
    })
    @Description(desc = "检查作业执行sql文件状态")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray sqlVoArray = paramObj.getJSONArray("sqlVoList");
        if (StringUtils.equals(paramObj.getString("operType"), AutoexecOperType.AUTOEXEC.getValue())) {
            Date nowLcd = new Date();
            if (CollectionUtils.isNotEmpty(sqlVoArray)) {
                for (AutoexecJobSqlDetailVo sqlDetailVo : sqlVoArray.toJavaList(AutoexecJobSqlDetailVo.class)) {
                    sqlDetailVo.setLcd(nowLcd);
                    autoexecJobMapper.insertJobSqlDetail(sqlDetailVo);
                }
            }
            List<Long> deleteSqlDetailList = autoexecJobMapper.getJobSqlDetailByJobIdAndLcd(paramObj.getLong("jobId"), nowLcd);
            if (CollectionUtils.isNotEmpty(deleteSqlDetailList)) {
                autoexecJobMapper.updateJobSqlIsDeleteByIdList(deleteSqlDetailList);
            }
        } else if (StringUtils.equals(paramObj.getString("operType"), DeployOperType.DEPLOY.getValue())) {
            IDeploySqlCrossoverMapper iDeploySqlCrossoverMapper = CrossoverServiceFactory.getApi(IDeploySqlCrossoverMapper.class);

            DeploySqlDetailVo deployVersionSql = new DeploySqlDetailVo(paramObj.getLong("systemId"), paramObj.getLong("moduleId"), paramObj.getLong("envId"), paramObj.getString("version"));
            List<DeploySqlDetailVo> allDeploySqlList = iDeploySqlCrossoverMapper.getAllJobDeploySqlDetailList(deployVersionSql);
            Map<String, DeploySqlDetailVo> allDeploySqlMap = allDeploySqlList.stream().collect(Collectors.toMap(DeploySqlDetailVo::getSqlFile, e -> e));
            List<Long> allSqlIdList = allDeploySqlList.stream().map(DeploySqlDetailVo::getId).collect(Collectors.toList());

            List<DeploySqlDetailVo> insertSqlList = new ArrayList<>();
            List<DeploySqlDetailVo> updateSqlList = new ArrayList<>();

            if (CollectionUtils.isNotEmpty(sqlVoArray)) {
                for (DeploySqlDetailVo newSqlVo : sqlVoArray.toJavaList(DeploySqlDetailVo.class)) {
                    DeploySqlDetailVo oldSqlVo = allDeploySqlMap.get(newSqlVo.getSqlFile());
                    if (oldSqlVo == null) {
                        insertSqlList.add(newSqlVo);
                        continue;
                    }
                    allSqlIdList.remove(oldSqlVo.getId());
                    if (oldSqlVo.getIsDelete() == 0 && StringUtils.equals(oldSqlVo.getStatus(), newSqlVo.getStatus()) && StringUtils.equals(oldSqlVo.getMd5(), newSqlVo.getMd5())) {
                        continue;
                    }
                    oldSqlVo.setStatus(newSqlVo.getStatus());
                    oldSqlVo.setMd5(newSqlVo.getMd5());
                    updateSqlList.add(oldSqlVo);
                }
                if (CollectionUtils.isNotEmpty(allSqlIdList)) {
                    iDeploySqlCrossoverMapper.updateJobDeploySqlIsDeleteByIdList(allSqlIdList);
                }
                if (CollectionUtils.isNotEmpty(insertSqlList)) {
                    for (DeploySqlDetailVo insertSqlVo : insertSqlList) {
                        iDeploySqlCrossoverMapper.insertDeploySql(new DeployJobSqlVo(paramObj.getLong("jobId"), insertSqlVo.getId()));
                        iDeploySqlCrossoverMapper.insertDeploySqlDetail(insertSqlVo);
                    }
                }
                if (CollectionUtils.isNotEmpty(updateSqlList)) {
                    for (DeploySqlDetailVo updateSqlVo : updateSqlList) {
                        iDeploySqlCrossoverMapper.updateJobDeploySqlDetailIsDeleteAndStatusAndMd5AndLcdById(updateSqlVo.getStatus(), updateSqlVo.getMd5(), updateSqlVo.getId());
                    }
                }
            }
        }
        return null;
    }
}
