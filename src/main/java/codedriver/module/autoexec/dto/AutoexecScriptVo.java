/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dto;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BaseEditorVo;
import codedriver.framework.restful.annotation.EntityField;
import codedriver.framework.util.SnowflakeUtil;

import java.util.List;

public class AutoexecScriptVo extends BaseEditorVo {

    @EntityField(name = "id", type = ApiParamType.LONG)
    private Long id;
    @EntityField(name = "唯一标识", type = ApiParamType.STRING)
    private String name;
    @EntityField(name = "名称", type = ApiParamType.STRING)
    private String label;
    @EntityField(name = "执行方式", type = ApiParamType.STRING)
    private String execMode;
    @EntityField(name = "分类ID", type = ApiParamType.LONG)
    private Long typeId;
    @EntityField(name = "操作级别ID", type = ApiParamType.LONG)
    private Long riskId;

    @EntityField(name = "分类名称", type = ApiParamType.STRING)
    private String type;
    @EntityField(name = "操作级别名称", type = ApiParamType.STRING)
    private String risk;
    @EntityField(name = "当前激活版本号", type = ApiParamType.INTEGER)
    private Integer currentVersion;
    @EntityField(name = "版本总数", type = ApiParamType.INTEGER)
    private Integer versionCount;
    @EntityField(name = "待审批版本数", type = ApiParamType.INTEGER)
    private Integer reviewingVersionCount;
    @EntityField(name = "已通过版本数", type = ApiParamType.INTEGER)
    private Integer reviewedVersionCount;

    @EntityField(name = "版本ID", type = ApiParamType.LONG)
    private Long versionId;
    @EntityField(name = "脚本解析器", type = ApiParamType.STRING)
    private String parser;
    @EntityField(name = "脚本配置信息", type = ApiParamType.STRING)
    private String config;
    @EntityField(name = "脚本内容行", type = ApiParamType.JSONARRAY)
    private List<AutoexecScriptLineVo> lineVoList;

    @EntityField(name = "版本号", type = ApiParamType.INTEGER)
    private Integer version;
    @EntityField(name = "参数列表", type = ApiParamType.JSONARRAY)
    private List<AutoexecScriptVersionParamVo> paramList;

    @EntityField(name = "脚本内容行", type = ApiParamType.JSONARRAY)
    private List<String> lineList;


    public AutoexecScriptVo() {
    }

    public Long getId() {
        if (id == null) {
            id = SnowflakeUtil.uniqueLong();
        }
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getExecMode() {
        return execMode;
    }

    public void setExecMode(String execMode) {
        this.execMode = execMode;
    }

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    public Long getRiskId() {
        return riskId;
    }

    public void setRiskId(Long riskId) {
        this.riskId = riskId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRisk() {
        return risk;
    }

    public void setRisk(String risk) {
        this.risk = risk;
    }

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Integer currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Integer getVersionCount() {
        return versionCount;
    }

    public void setVersionCount(Integer versionCount) {
        this.versionCount = versionCount;
    }

    public Integer getReviewingVersionCount() {
        return reviewingVersionCount;
    }

    public void setReviewingVersionCount(Integer reviewingVersionCount) {
        this.reviewingVersionCount = reviewingVersionCount;
    }

    public Integer getReviewedVersionCount() {
        return reviewedVersionCount;
    }

    public void setReviewedVersionCount(Integer reviewedVersionCount) {
        this.reviewedVersionCount = reviewedVersionCount;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public String getParser() {
        return parser;
    }

    public void setParser(String parser) {
        this.parser = parser;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public List<AutoexecScriptLineVo> getLineVoList() {
        return lineVoList;
    }

    public void setLineVoList(List<AutoexecScriptLineVo> lineVoList) {
        this.lineVoList = lineVoList;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<AutoexecScriptVersionParamVo> getParamList() {
        return paramList;
    }

    public void setParamList(List<AutoexecScriptVersionParamVo> paramList) {
        this.paramList = paramList;
    }

    public List<String> getLineList() {
        return lineList;
    }

    public void setLineList(List<String> lineList) {
        this.lineList = lineList;
    }
}
