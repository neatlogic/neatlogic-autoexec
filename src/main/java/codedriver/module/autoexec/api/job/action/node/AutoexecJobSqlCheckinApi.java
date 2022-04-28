package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobSqlDetailVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobSqlVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
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
            @Param(name = "sqlVoList", type = ApiParamType.JSONARRAY, desc = "作业sql文件列表"),
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id"),
            @Param(name = "systemId", type = ApiParamType.LONG, desc = "系统id"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id"),
            @Param(name = "version", type = ApiParamType.LONG, desc = "版本"),
            @Param(name = "sqlVoList", type = ApiParamType.JSONARRAY, desc = "作业sql文件列表"),
            @Param(name = "operType", type = ApiParamType.STRING, isRequired = true, desc = "标记")
    })
    @Output({
    })
    @Description(desc = "检查作业执行sql文件状态")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray sqlVoArray = paramObj.getJSONArray("sqlVoList");

        if (StringUtils.equals(paramObj.getString("operType"), AutoexecJobSqlOperType.AUTO.getValue())) {
            List<Long> allSqlIdList = new ArrayList<>();
            List<AutoexecJobSqlDetailVo> insertSqlList = new ArrayList<>();
            List<AutoexecJobSqlDetailVo> updateSqlList = new ArrayList<>();

            List<AutoexecJobSqlDetailVo> allJobSqlList = autoexecJobMapper.getJobAllSqlDetail(paramObj.getLong("jobId"));
            Map<Long, AutoexecJobSqlDetailVo> allJobSqlVoMap = allJobSqlList.stream().collect(Collectors.toMap(AutoexecJobSqlDetailVo::getNodeId, e -> e));
            if (CollectionUtils.isNotEmpty(allJobSqlList)) {
                allSqlIdList = allJobSqlList.stream().map(AutoexecJobSqlDetailVo::getId).collect(Collectors.toList());
            }

            if (CollectionUtils.isNotEmpty(sqlVoArray)) {
                for (AutoexecJobSqlDetailVo newSqlVo : sqlVoArray.toJavaList(AutoexecJobSqlDetailVo.class)) {
                    AutoexecJobSqlDetailVo oldSqlVo = allJobSqlVoMap.get(newSqlVo.getNodeId());
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
            }

            if (CollectionUtils.isNotEmpty(allSqlIdList)) {
                autoexecJobMapper.updateJobSqlIsDeleteByIdList(allSqlIdList);
            }
            if (CollectionUtils.isNotEmpty(insertSqlList)) {
                for (AutoexecJobSqlDetailVo insertSqlVo : insertSqlList) {
                    autoexecJobMapper.insertAutoexecJobSql(new AutoexecJobSqlVo(paramObj.getLong("jobId"), insertSqlVo.getId()));
                    autoexecJobMapper.insertJobSqlDetail(insertSqlVo);
                }
            }
            if (CollectionUtils.isNotEmpty(updateSqlList)) {
                for (AutoexecJobSqlDetailVo updateSqlVo : updateSqlList) {
                    autoexecJobMapper.updateJobSqlDetailIsDeleteAndStatusAndMd5AndLcdById(updateSqlVo.getStatus(), updateSqlVo.getMd5(), updateSqlVo.getId());
                }
            }
        } else if (StringUtils.equals(paramObj.getString("operType"), AutoexecJobSqlOperType.DEPLOY.getValue())) {
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
//                        autoexecJobMapper.insertAutoexecJobSql(new AutoexecJobSqlVo(jobId, insertSqlVo.getId()));
//                        autoexecJobMapper.insertJobSqlDetail(insertSqlVo);
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
