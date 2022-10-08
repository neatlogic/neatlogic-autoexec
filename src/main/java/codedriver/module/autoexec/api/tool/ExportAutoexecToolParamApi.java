/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.autoexec.api.tool;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_BASE;
import codedriver.framework.autoexec.constvalue.ParamDataSource;
import codedriver.framework.autoexec.constvalue.ParamType;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.exception.AutoexecToolExportNotFoundToolException;
import codedriver.framework.autoexec.exception.AutoexecToolNotFoundException;
import codedriver.framework.cmdb.crossover.IResourceAccountCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.file.dto.FileVo;
import codedriver.framework.matrix.core.MatrixPrivateDataSourceHandlerFactory;
import codedriver.framework.matrix.dao.mapper.MatrixMapper;
import codedriver.framework.matrix.dto.MatrixVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.word.WordBuilder;
import codedriver.framework.util.word.enums.FontFamily;
import codedriver.framework.util.word.enums.TitleType;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/9/20 11:26
 */

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportAutoexecToolParamApi extends PrivateBinaryStreamApiComponentBase {

    private static final Log logger = LogFactory.getLog(ExportAutoexecToolParamApi.class);

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Resource
    private MatrixMapper matrixMapper;

    @Override
    public String getName() {
        return "导出工具库工具参数";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/tool/param/export";
    }

    @Input({
            @Param(name = "toolId", type = ApiParamType.LONG, desc = "工具id"),
            @Param(name = "isAll", type = ApiParamType.INTEGER, isRequired = true, desc = "是否全量（1：全量，0：单个）")
    })
    @Description(desc = "导出工具库工具参数")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {

        Long toolId = paramObj.getLong("toolId");
        Integer isAll = paramObj.getInteger("isAll");

        //获取fileName、需要导出的toolVoList
        List<AutoexecToolVo> toolVoList = new ArrayList<>();
        String fileName = "";
        if (isAll == 1) {
            fileName = "自动化工具库";
            toolVoList = autoexecToolMapper.getAllTool();
        } else if (isAll == 0) {
            if (toolId == null) {
                throw new ParamNotExistsException("toolId");
            }
            AutoexecToolVo toolVo = autoexecToolMapper.getToolById(toolId);
            if (toolVo == null) {
                throw new AutoexecToolNotFoundException(toolId);
            }
            fileName = toolVo.getName();
            toolVoList.add(toolVo);
        }
        if (CollectionUtils.isEmpty(toolVoList)) {
            throw new AutoexecToolExportNotFoundToolException();
        }

        //导出
        OutputStream os = null;
        try {
            os = response.getOutputStream();
            response.setContentType("application/x-download");
            response.setHeader("Content-Disposition",
                    " attachment; filename=\"[" + URLEncoder.encode(fileName + "]参数说明", "utf-8") + ".docx\"");
            WordBuilder wordBuilder = new WordBuilder();
            Map<Integer, String> tableHeaderMap = new HashMap<>();
            tableHeaderMap.put(1, "参数名");
            tableHeaderMap.put(2, "控件类型");
            tableHeaderMap.put(3, "必填/选填");
            tableHeaderMap.put(4, "默认值");
            tableHeaderMap.put(5, "描述");

            //工具类型分类
            Map<String, List<AutoexecToolVo>> allTypeAutoexecToolListMap = toolVoList.stream().collect(Collectors.groupingBy(e -> e.getTypeName() + "[" + e.getTypeDescription() + "]"));
            int typeNum = 1;
            for (String type : allTypeAutoexecToolListMap.keySet()) {
                wordBuilder.addTitle(TitleType.H2, "1." + typeNum + "  " + type);
                List<AutoexecToolVo> toolVos = allTypeAutoexecToolListMap.get(type);
                for (int toolNum = 1; toolNum <= toolVos.size(); toolNum++) {
                    AutoexecToolVo toolVo = toolVos.get(toolNum - 1);
                    //TODO cmdbcollect/fcswitchcollector这个工具的dataList还有问题，等波哥改为才可以支持这个工具的导出
                    if (Objects.equals(toolVo.getName(), "cmdbcollect/fcswitchcollector")) {
                        continue;
                    }

                    wordBuilder.addTitle(TitleType.H3, "1." + typeNum + "." + toolNum + "  " + toolVo.getName());
                    wordBuilder.addParagraph("描述：" + toolVo.getDescription()).setFontSize(12).setFontFamily(FontFamily.REGULAR_SCRIPT.getValue());
                    wordBuilder.addParagraph("执行方式：" + toolVo.getExecModeText()).setFontSize(12).setFontFamily(FontFamily.REGULAR_SCRIPT.getValue());
                    if (CollectionUtils.isEmpty(toolVo.getInputParamList())) {
                        wordBuilder.addParagraph("无参数").setFontSize(12).setFontFamily(FontFamily.REGULAR_SCRIPT.getValue());
                        continue;
                    }
                    wordBuilder.addParagraph("参数：").setFontSize(12).setFontFamily(FontFamily.REGULAR_SCRIPT.getValue());
                    List<Map<String, String>> list = new ArrayList<>();
                    //表格数据（参数数据）
                    for (AutoexecParamVo paramVo : toolVo.getInputParamList()) {
                        Map<String, String> map = new HashMap<>();
                        map.put("参数名", paramVo.getName() + "（" + paramVo.getKey() + "）");
                        map.put("控件类型", paramVo.getTypeText());
                        map.put("必填/选填", ((paramVo.getIsRequired() != null && paramVo.getIsRequired() == 1) ? "必填" : "选填"));
                        map.put("默认值", new String(getDefaultValue(paramVo)));
                        map.put("描述", paramVo.getDescription());
                        list.add(map);
                    }
                    wordBuilder.addTable(tableHeaderMap).addRows(list);
                    wordBuilder.addBlankRow();
                }
                typeNum++;
            }
            wordBuilder.builder().write(os);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            if (os != null) {
                os.flush();
                os.close();
            }
        }
        return null;
    }

    /**
     * 获取默认值
     *
     * @param paramVo 参数vo
     * @return 用于页面回显的默认值
     */
    StringBuilder getDefaultValue(AutoexecParamVo paramVo) {
        //返回用于页面显示的 默认值
        StringBuilder returnDefaultValue = new StringBuilder();
        //参数的默认值（可能是string、List类型，可能是映射关系）
        Object paramDefaultValue = paramVo.getDefaultValue();

        //单选、单选下拉
        if (StringUtils.equals(ParamType.RADIO.getValue(), paramVo.getType()) || StringUtils.equals(ParamType.SELECT.getValue(), paramVo.getType())) {

            //静态数据源
            if (StringUtils.equals(ParamDataSource.STATIC.getValue(), paramVo.getConfig().getString("dataSource"))) {
                returnDefaultValue = new StringBuilder("静态");
                if (paramDefaultValue != null && paramDefaultValue != "") {
                    JSONArray dataArray = paramVo.getConfig().getJSONArray("dataList");
                    if (CollectionUtils.isNotEmpty(dataArray)) {
                        List<ValueTextVo> valueTextVoList = dataArray.toJavaList(ValueTextVo.class);
                        Map<Object, String> valueTextMap = valueTextVoList.stream().collect(Collectors.toMap(ValueTextVo::getValue, ValueTextVo::getText));
                        returnDefaultValue.append("  ").append(valueTextMap.get(paramDefaultValue));
                    }
                }
                //矩阵数据源
            } else if (StringUtils.equals(ParamDataSource.MATRIX.getValue(), paramVo.getConfig().getString("dataSource"))) {
                String matrixUuid = paramVo.getConfig().getString("matrixUuid");
                if (StringUtils.isNotBlank(matrixUuid)) {
                    MatrixVo matrixVo = matrixMapper.getMatrixByUuid(matrixUuid);
                    if (matrixVo == null) {
                        matrixVo = MatrixPrivateDataSourceHandlerFactory.getMatrixVo(matrixUuid);
                    }
                    if (matrixVo != null) {
                        returnDefaultValue = new StringBuilder(matrixVo.getName());
                        if (paramDefaultValue != null) {
                            String valueString = String.valueOf(paramDefaultValue);
                            returnDefaultValue.append("  ").append(valueString.substring(valueString.substring(0, valueString.indexOf("&=&")).length() + 3));
                        }
                    }
                }
            }

            //复选、多选下拉
        } else if (StringUtils.equals(ParamType.CHECKBOX.getValue(), paramVo.getType()) || StringUtils.equals(ParamType.MULTISELECT.getValue(), paramVo.getType())) {
            //静态数据源
            if (StringUtils.equals(ParamDataSource.STATIC.getValue(), paramVo.getConfig().getString("dataSource"))) {
                returnDefaultValue = new StringBuilder("静态");
                if (paramDefaultValue != null) {
                    List<Object> valueList = (List<Object>) paramDefaultValue;
                    JSONArray dataArray = paramVo.getConfig().getJSONArray("dataList");
                    if (CollectionUtils.isNotEmpty(dataArray)) {
                        List<ValueTextVo> valueTextVoList = dataArray.toJavaList(ValueTextVo.class);
                        Map<Object, String> valueTextMap = valueTextVoList.stream().collect(Collectors.toMap(ValueTextVo::getValue, ValueTextVo::getText));
                        for (int i = 0; i < valueList.size(); i++) {
                            Object object = valueList.get(i);
                            returnDefaultValue.append("  ").append(valueTextMap.get(object));
                            if (i < valueList.size() - 1) {
                                returnDefaultValue.append("|");
                            }
                        }
                    }
                }

                //矩阵数据源
            } else if (StringUtils.equals(ParamDataSource.MATRIX.getValue(), paramVo.getConfig().getString("dataSource"))) {
                String matrixUuid = paramVo.getConfig().getString("matrixUuid");
                if (StringUtils.isNotBlank(matrixUuid)) {
                    MatrixVo matrixVo = matrixMapper.getMatrixByUuid(matrixUuid);
                    if (matrixVo == null) {
                        matrixVo = MatrixPrivateDataSourceHandlerFactory.getMatrixVo(matrixUuid);
                    }
                    if (matrixVo != null) {
                        returnDefaultValue = new StringBuilder(matrixVo.getName());
                        if (paramDefaultValue != null) {
                            List<Object> valueList = (List<Object>) paramDefaultValue;
                            returnDefaultValue.append("  ");
                            for (int i = 0; i < valueList.size(); i++) {
                                String valueString = (String) valueList.get(i);
                                returnDefaultValue.append(valueString.substring(valueString.substring(0, valueString.indexOf("&=&")).length() + 3));
                                if (i < valueList.size() - 1) {
                                    returnDefaultValue.append("|");
                                }
                            }
                        }
                    }
                }
            }

            //文件
        } else if (StringUtils.equals(ParamType.FILE.getValue(), paramVo.getType())) {
            if (paramDefaultValue != null) {
                JSONObject jsonObject = (JSONObject) paramDefaultValue;
                JSONArray fileArray = jsonObject.getJSONArray("fileList");
                if (CollectionUtils.isNotEmpty(fileArray)) {
                    List<FileVo> fileVoList = fileArray.toJavaList(FileVo.class);
                    if (CollectionUtils.isNotEmpty(fileVoList)) {
                        returnDefaultValue = new StringBuilder(fileVoList.stream().map(FileVo::getName).collect(Collectors.joining("  ")));
                    }
                }
            }

            //节点
        } else if (StringUtils.equals(ParamType.NODE.getValue(), paramVo.getType())) {
            if (paramDefaultValue != null) {
                JSONArray jsonArray = (JSONArray) paramDefaultValue;
                if (CollectionUtils.isNotEmpty(jsonArray)) {
                    for (int i = 0; i < jsonArray.size(); i++) {
                        Object object = jsonArray.get(i);
                        JSONObject jsonObject = (JSONObject) object;
                        ResourceVo resourceVo = jsonObject.toJavaObject(ResourceVo.class);
                        returnDefaultValue.append(resourceVo.getIp()).append(":").append(resourceVo.getPort()).append("/").append(resourceVo.getName());
                        if (i < jsonArray.size() - 1) {
                            returnDefaultValue.append("|");
                        }
                    }

                }
            }

            //账号
        } else if (StringUtils.equals(ParamType.ACCOUNT.getValue(), paramVo.getType())) {
            if (paramDefaultValue != null && paramDefaultValue != "") {
                IResourceAccountCrossoverMapper resourceAccountCrossoverMapper = CrossoverServiceFactory.getApi(IResourceAccountCrossoverMapper.class);
                AccountVo accountVo = resourceAccountCrossoverMapper.getAccountById(Long.valueOf(paramDefaultValue.toString()));
                if (accountVo != null) {
                    returnDefaultValue.append(accountVo.getName()).append("(").append(accountVo.getAccount()).append(")/").append(accountVo.getProtocol());
                }
            }

            //开关
        } else if (StringUtils.equals(ParamType.SWITCH.getValue(), paramVo.getType())) {
            if (paramDefaultValue != null) {
                if (StringUtils.equals(paramDefaultValue.toString(), "false")) {
                    returnDefaultValue = new StringBuilder("否");
                } else if (StringUtils.equals(paramDefaultValue.toString(), "ture")) {
                    returnDefaultValue = new StringBuilder("是");
                }
            }
        } else {
            if (paramDefaultValue != null) {
                returnDefaultValue = new StringBuilder(paramDefaultValue.toString());
            }
        }
        return returnDefaultValue;
    }

}