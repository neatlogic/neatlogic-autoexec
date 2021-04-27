package codedriver.module.autoexec.api.combop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_COMBOP_MODIFY;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopParamVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopVo;
import codedriver.framework.autoexec.exception.AutoexecCombopNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.PermissionDeniedException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecCombopMapper;
import codedriver.module.autoexec.service.AutoexecCombopService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 导出组合工具接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@AuthAction(action = AUTOEXEC_COMBOP_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecCombopExportApi extends PrivateBinaryStreamApiComponentBase {
	
	@Autowired
	private AutoexecCombopMapper autoexecCombopMapper;

	@Resource
	private AutoexecCombopService autoexecCombopService;
	
	@Override
	public String getToken() {
		return "autoexec/combop/export";
	}

	@Override
	public String getName() {
		return "导出组合工具";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({
		@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "主键id")
	})
	@Description(desc = "导出组合工具")
	@Override
	public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Long id = paramObj.getLong("id");
		AutoexecCombopVo autoexecCombopVo = autoexecCombopMapper.getAutoexecCombopById(id);
		if (autoexecCombopVo == null) {
			throw new AutoexecCombopNotFoundException(id);
		}
		autoexecCombopService.setOperableButtonList(autoexecCombopVo);
		if (autoexecCombopVo.getEditable() == 0) {
			throw new PermissionDeniedException();
		}
		List<AutoexecCombopParamVo> runtimeParamList = autoexecCombopMapper.getAutoexecCombopParamListByCombopId(id);
		autoexecCombopVo.setRuntimeParamList(runtimeParamList);
		//设置导出文件名
		String fileNameEncode = autoexecCombopVo.getName() + ".combop";
		Boolean flag = request.getHeader("User-Agent").indexOf("Gecko") > 0;
		if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0 || flag) {
			fileNameEncode = URLEncoder.encode(fileNameEncode, "UTF-8");// IE浏览器
		} else {
			fileNameEncode = new String(fileNameEncode.replace(" ", "").getBytes(StandardCharsets.UTF_8), "ISO8859-1");
		}
		response.setContentType("aplication/x-msdownload");
		
		response.setHeader("Content-Disposition", "attachment;fileName=\"" + fileNameEncode + "\"");
		//获取序列化字节数组
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(autoexecCombopVo);
		ServletOutputStream os = null;
		os = response.getOutputStream();
		IOUtils.write(baos.toByteArray(), os);
		if (os != null) {
			os.flush();
			os.close();
		}
		if(oos != null) {
			oos.close();
		}
		if(baos != null) {
			baos.close();
		}
		return null;
	}

}
