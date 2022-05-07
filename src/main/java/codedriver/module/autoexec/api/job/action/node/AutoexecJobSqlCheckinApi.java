package codedriver.module.autoexec.api.job.action.node;

import codedriver.framework.autoexec.constvalue.AutoexecOperType;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecSqlDetailVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.constvalue.DeployOperType;
import codedriver.framework.deploy.crossover.IDeploySqlCrossoverMapper;
import codedriver.framework.deploy.dto.sql.DeploySqlDetailVo;
import codedriver.framework.deploy.dto.sql.DeploySqlVo;
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
import java.util.*;
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
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业id"),
            @Param(name = "systemId", type = ApiParamType.LONG, desc = "系统id"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块id"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境id"),
            @Param(name = "version", type = ApiParamType.LONG, desc = "版本"),
            @Param(name = "operType", type = ApiParamType.ENUM, rule = "auto,deploy", isRequired = true, desc = "来源类型")
    })
    @Output({
    })
    @Description(desc = "检查作业执行sql文件状态")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray paramSqlVoArray = paramObj.getJSONArray("sqlVoList");
        if (StringUtils.equals(paramObj.getString("operType"), AutoexecOperType.AUTOEXEC.getValue())) {
            Date nowLcd = new Date();
            if (CollectionUtils.isNotEmpty(paramSqlVoArray)) {
                List<AutoexecSqlDetailVo> insertSqlList = paramSqlVoArray.toJavaList(AutoexecSqlDetailVo.class);
                if (insertSqlList.size() > 100) {
                    int cyclicNumber = insertSqlList.size() / 100;
                    if (insertSqlList.size() % 100 != 0) {
                        cyclicNumber++;
                    }
                    for (int i = 0; i < cyclicNumber; i++) {
                        autoexecJobMapper.insertSqlDetailList(insertSqlList.subList(i * 100, (Math.min((i + 1) * 100, insertSqlList.size()))), nowLcd);
                    }
                } else {
                    autoexecJobMapper.insertSqlDetailList(insertSqlList, nowLcd);
                }
            }
            List<Long> needDeleteSqlIdList = autoexecJobMapper.getSqlDetailByJobIdAndLcd(paramObj.getLong("jobId"), nowLcd);
            if (CollectionUtils.isNotEmpty(needDeleteSqlIdList)) {
                autoexecJobMapper.updateSqlIsDeleteByIdList(needDeleteSqlIdList);
            }
        } else if (StringUtils.equals(paramObj.getString("operType"), DeployOperType.DEPLOY.getValue())) {
            IDeploySqlCrossoverMapper iDeploySqlCrossoverMapper = CrossoverServiceFactory.getApi(IDeploySqlCrossoverMapper.class);

            DeploySqlDetailVo deployVersionSql = new DeploySqlDetailVo(paramObj.getLong("systemId"), paramObj.getLong("moduleId"), paramObj.getLong("envId"), paramObj.getString("version"));
            List<DeploySqlDetailVo> oldDeploySqlList = iDeploySqlCrossoverMapper.getAllDeploySqlDetailList(deployVersionSql);
            Map<String, DeploySqlDetailVo> oldDeploySqlMap = new HashMap<>();
            List<Long> needDeleteSqlIdList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(oldDeploySqlList)) {
                oldDeploySqlMap = oldDeploySqlList.stream().collect(Collectors.toMap(DeploySqlDetailVo::getSqlFile, e -> e));
                needDeleteSqlIdList = oldDeploySqlList.stream().map(DeploySqlDetailVo::getId).collect(Collectors.toList());
            }
            List<DeploySqlDetailVo> insertSqlList = new ArrayList<>();
            List<DeploySqlDetailVo> updateSqlList = new ArrayList<>();

            if (CollectionUtils.isNotEmpty(paramSqlVoArray)) {
                for (DeploySqlDetailVo newSqlVo : paramSqlVoArray.toJavaList(DeploySqlDetailVo.class)) {
                    DeploySqlDetailVo oldSqlVo = oldDeploySqlMap.get(newSqlVo.getSqlFile());
                    //不存在则新增
                    if (oldSqlVo == null) {
                        insertSqlList.add(newSqlVo);
                        continue;
                    }
                    if (CollectionUtils.isNotEmpty(needDeleteSqlIdList)) {
                        //旧数据 - 需要更新的数据 = 需要删除的数据
                        needDeleteSqlIdList.remove(oldSqlVo.getId());
                    }
                    if (oldSqlVo.getIsDelete() == 0 && StringUtils.equals(oldSqlVo.getStatus(), newSqlVo.getStatus()) && StringUtils.equals(oldSqlVo.getMd5(), newSqlVo.getMd5())) {
                        continue;
                    }
                    //需要更新的数据
                    newSqlVo.setId(oldSqlVo.getId());
                    updateSqlList.add(newSqlVo);
                }
                if (CollectionUtils.isNotEmpty(needDeleteSqlIdList)) {
                    iDeploySqlCrossoverMapper.updateDeploySqlIsDeleteByIdList(needDeleteSqlIdList);
                }
                if (CollectionUtils.isNotEmpty(insertSqlList)) {
                    for (DeploySqlDetailVo insertSqlVo : insertSqlList) {
                        iDeploySqlCrossoverMapper.insertDeploySql(new DeploySqlVo(paramObj.getLong("jobId"), insertSqlVo.getId()));
                        iDeploySqlCrossoverMapper.insertDeploySqlDetail(insertSqlVo);
                    }
                }
                if (CollectionUtils.isNotEmpty(updateSqlList)) {
                    for (DeploySqlDetailVo updateSqlVo : updateSqlList) {
                        iDeploySqlCrossoverMapper.updateDeploySqlDetailIsDeleteAndStatusAndMd5ById(updateSqlVo.getStatus(), updateSqlVo.getMd5(), updateSqlVo.getId());
                    }
                }
            }
        }
        return null;
    }
}
