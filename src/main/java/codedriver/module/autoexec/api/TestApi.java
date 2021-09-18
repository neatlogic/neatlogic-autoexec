package codedriver.module.autoexec.api;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service

@Transactional
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class TestApi extends PrivateApiComponentBase{
	@Resource
	AutoexecJobMapper jobMapper;

	@Override
	public String getToken() {
		return "autoexec/test";
	}

	@Override
	public String getName() {
		return "test";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Description(desc = "test")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		jobMapper.updateJobStatus(new AutoexecJobVo(452608720166912L, JobStatus.COMPLETED.getValue()));
		return "asdf";
	}

}
