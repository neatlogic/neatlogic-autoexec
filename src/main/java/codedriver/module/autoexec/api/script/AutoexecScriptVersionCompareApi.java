/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import codedriver.framework.autoexec.constvalue.ChangeType;
import codedriver.framework.autoexec.constvalue.ScriptVersionStatus;
import codedriver.framework.autoexec.dto.script.AutoexecScriptLineVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.dao.mapper.RoleMapper;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.RoleVo;
import codedriver.framework.dto.UserVo;
import codedriver.framework.dto.WorkAssignmentUnitVo;
import codedriver.framework.lcs.LCSUtil;
import codedriver.framework.lcs.SegmentPair;
import codedriver.framework.lcs.SegmentRange;
import codedriver.framework.matrix.dao.mapper.MatrixAttributeMapper;
import codedriver.framework.matrix.dao.mapper.MatrixMapper;
import codedriver.framework.matrix.dto.MatrixAttributeVo;
import codedriver.framework.matrix.dto.MatrixVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.service.AutoexecScriptService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_SEARCH.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptVersionCompareApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptService autoexecScriptService;

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private MatrixMapper matrixMapper;

    @Resource
    private MatrixAttributeMapper matrixAttributeMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;

    @Override
    public String getToken() {
        return "autoexec/script/version/compare";
    }

    @Override
    public String getName() {
        return "脚本版本对比";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public boolean disableReturnCircularReferenceDetect() {
        return true;
    }

    @Input({
            @Param(name = "sourceVersionId", type = ApiParamType.LONG, isRequired = true, desc = "源版本ID"),
            @Param(name = "targetVersionId", type = ApiParamType.LONG, desc = "目标版本ID(查看待审核版本时，默认进入对比页，故无需传目标版本ID，只有手动对比时才需要)"),
            @Param(name = "needToCompare", type = ApiParamType.ENUM, rule = "0,1", desc = "是否需要对比(不传时默认开启对比，值为0时关闭对比，但targetVersionId不为空时，一定会对比)"),
    })
    @Output({
            @Param(name = "sourceVersion", explode = AutoexecScriptVersionVo[].class, desc = "源版本"),
            @Param(name = "targetVersion", explode = AutoexecScriptVersionVo[].class, desc = "目标版本"),
    })
    @Description(desc = "脚本版本对比")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        /**
         * 如果targetVersionId不为空，表示点击对比按钮调用的此接口
         * 如果targetVersionId为空，表示查看待审批版本，显示对比页，
         * 此时对比目标为当前激活版本，如果没有当前激活版本，那么不返回targetVersion
         */
        JSONObject result = new JSONObject();
        Long sourceVersionId = jsonObj.getLong("sourceVersionId");
        Long targetVersionId = jsonObj.getLong("targetVersionId");
        Integer needToCompare = jsonObj.getInteger("needToCompare");
        AutoexecScriptVersionVo sourceVersion = autoexecScriptService.getScriptVersionDetailByVersionId(sourceVersionId);
        AutoexecScriptVersionVo targetVersion = null;
        if (targetVersionId != null) {
            targetVersion = autoexecScriptService.getScriptVersionDetailByVersionId(targetVersionId);
        } else if (targetVersionId == null && !Objects.equals(needToCompare, 0)) {
            // 查询拥有脚本审批权限的人和角色
            List<RoleVo> roleList = roleMapper.getRoleListByAuthName(AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName());
            List<UserVo> userList = userMapper.searchUserByAuth(AUTOEXEC_SCRIPT_MANAGE.class.getSimpleName());
            if (CollectionUtils.isNotEmpty(roleList) || CollectionUtils.isNotEmpty(userList)) {
                List<WorkAssignmentUnitVo> reviewerVoList = new ArrayList<>();
                sourceVersion.setReviewerVoList(reviewerVoList);
                if (CollectionUtils.isNotEmpty(roleList)) {
                    for (RoleVo vo : roleList) {
                        reviewerVoList.add(new WorkAssignmentUnitVo(vo));
                    }
                }
                if (CollectionUtils.isNotEmpty(userList)) {
                    for (UserVo vo : userList) {
                        reviewerVoList.add(new WorkAssignmentUnitVo(vo));
                    }
                }
            }
            // 查询当前激活版本作为对比目标
            AutoexecScriptVersionVo activeVersion = autoexecScriptMapper.getActiveVersionByScriptId(sourceVersion.getScriptId());
            if (activeVersion != null) {
                targetVersion = autoexecScriptService.getScriptVersionDetailByVersionId(activeVersion.getId());
            }
        }
        result.put("sourceVersion", sourceVersion);
        if (targetVersion != null) {
            result.put("targetVersion", targetVersion);
            if (!Objects.equals(sourceVersionId, targetVersionId)) {
                compareScriptVersion(targetVersion, sourceVersion);
            }
            convertMatrixText(targetVersion.getParamList());
        }
        if (Objects.equals(sourceVersion.getStatus(), ScriptVersionStatus.SUBMITTED.getValue())) {
            sourceVersion.setOperateList(autoexecScriptService.getOperateListForScriptVersion(sourceVersion));
        }
        convertMatrixText(sourceVersion.getParamList());
        return result;
    }

    /**
     * 脚本版本对比，对比内容包括出参入参、解析器、脚本内容
     *
     * @param source 源版本
     * @param target 目标版本
     */
    private void compareScriptVersion(AutoexecScriptVersionVo source, AutoexecScriptVersionVo target) {
        List<AutoexecScriptVersionParamVo> sourceInputParamList = source.getInputParamList() != null ? source.getInputParamList() : new ArrayList<>();
        List<AutoexecScriptVersionParamVo> sourceOutputParamList = source.getOutputParamList() != null ? source.getOutputParamList() : new ArrayList<>();
        List<AutoexecScriptVersionParamVo> targetInputParamList = target.getInputParamList() != null ? target.getInputParamList() : new ArrayList<>();
        List<AutoexecScriptVersionParamVo> targetOutputParamList = target.getOutputParamList() != null ? target.getOutputParamList() : new ArrayList<>();
        compareParamList(targetInputParamList, sourceInputParamList);
        compareParamList(targetOutputParamList, sourceOutputParamList);
        if (!Objects.equals(source.getParser(), target.getParser())) {
            source.setParser("<span class='update'>" + source.getParser() + "</span>");
            target.setParser("<span class='update'>" + target.getParser() + "</span>");
        }
        compareLineList(source, target);
    }

    /**
     * 参数列表对比
     * 以source为准
     * 如果source为空，说明target被删除
     * 如果target为空，说明source是新增
     * 如果source比target长，说明新增了部分参数
     * 如果source比target短，说明删除了部分参数
     *
     * @param source
     * @param target
     */
    private void compareParamList(List<AutoexecScriptVersionParamVo> source, List<AutoexecScriptVersionParamVo> target) {
        if (source.size() != target.size()) {
            if (CollectionUtils.isEmpty(source)) {
                target.forEach(o -> o.setChangeType(ChangeType.DELETE.getValue()));
            } else if (CollectionUtils.isEmpty(target)) {
                source.forEach(o -> o.setChangeType(ChangeType.INSERT.getValue()));
            } else {
                // 对比行数相等部分的参数
                compareSameOrderParamList(source, target);
                if (source.size() > target.size()) {
                    // 将多余部分的参数标记为insert
                    for (int i = target.size(); i < source.size(); i++) {
                        source.get(i).setChangeType(ChangeType.INSERT.getValue());
                    }
                } else {
                    // 将多余部分的参数标记为delete
                    for (int i = source.size(); i < target.size(); i++) {
                        target.get(i).setChangeType(ChangeType.DELETE.getValue());
                    }
                }
            }
        } else {
            compareSameOrderParamList(source, target);
        }
    }

    /**
     * 以较短的参数列表的长度为准，对比行数相同的部分
     *
     * @param source
     * @param target
     */
    private void compareSameOrderParamList(List<AutoexecScriptVersionParamVo> source, List<AutoexecScriptVersionParamVo> target) {
        int size = Math.min(source.size(), target.size());
        for (int i = 0; i < size; i++) {
            AutoexecScriptVersionParamVo beforeNextParam = source.get(i);
            AutoexecScriptVersionParamVo afterNextParam = target.get(i);
            if (!Objects.equals(beforeNextParam, afterNextParam)) {
                beforeNextParam.setChangeType(ChangeType.UPDATE.getValue());
                afterNextParam.setChangeType(ChangeType.UPDATE.getValue());
            }
        }
    }

    /**
     * 对比脚本每行内容
     *
     * @param source
     * @param target
     */
    private void compareLineList(AutoexecScriptVersionVo source, AutoexecScriptVersionVo target) {
        List<AutoexecScriptLineVo> sourceLineList = source.getLineList();
        List<AutoexecScriptLineVo> targetLineList = target.getLineList();
        List<AutoexecScriptLineVo> sourceResultList = new ArrayList<>();
        List<AutoexecScriptLineVo> targetResultList = new ArrayList<>();
        List<SegmentPair> segmentPairList = LCSUtil.LCSCompare(sourceLineList, targetLineList);
        for (SegmentPair segmentPair : segmentPairList) {
            regroupLineList(sourceLineList, targetLineList, sourceResultList, targetResultList, segmentPair);
        }
        source.setLineList(sourceResultList);
        target.setLineList(targetResultList);
    }

    private void regroupLineList(List<AutoexecScriptLineVo> oldDataList, List<AutoexecScriptLineVo> newDataList
            , List<AutoexecScriptLineVo> oldResultList, List<AutoexecScriptLineVo> newResultList, SegmentPair segmentPair) {
        List<AutoexecScriptLineVo> oldSubList = oldDataList.subList(segmentPair.getOldBeginIndex(), segmentPair.getOldEndIndex());
        List<AutoexecScriptLineVo> newSubList = newDataList.subList(segmentPair.getNewBeginIndex(), segmentPair.getNewEndIndex());
        if (segmentPair.isMatch()) {
            /** 分段对匹配时，行数据不能做标记，直接添加到重组后的数据列表中 **/
            oldResultList.addAll(oldSubList);
            newResultList.addAll(newSubList);
        } else {
            /** 分段对不匹配时，分成下列四种情况 **/
            if (CollectionUtils.isEmpty(newSubList)) {
                /** 删除行 **/
                for (AutoexecScriptLineVo lineVo : oldSubList) {
                    lineVo.setChangeType(ChangeType.DELETE.getValue());
                    oldResultList.add(lineVo);
                    newResultList.add(createFillBlankLine(lineVo));
                }
            } else if (CollectionUtils.isEmpty(oldSubList)) {
                /** 插入行 **/
                for (AutoexecScriptLineVo lineVo : newSubList) {
                    oldResultList.add(createFillBlankLine(lineVo));
                    lineVo.setChangeType(ChangeType.INSERT.getValue());
                    newResultList.add(lineVo);
                }
            } else if (oldSubList.size() == 1 && newSubList.size() == 1) {
                /** 修改一行 **/
                AutoexecScriptLineVo oldLine = oldSubList.get(0);
                AutoexecScriptLineVo newLine = newSubList.get(0);
                if (!Objects.equals(oldLine.getContent(), newLine.getContent())) {
                    oldLine.setChangeType(ChangeType.UPDATE.getValue());
                    newLine.setChangeType(ChangeType.UPDATE.getValue());
                    if (StringUtils.length(oldLine.getContent()) == 0) {
                        newLine.setContent("<span class='insert'>" + newLine.getContent() + "</span>");
                    } else if (StringUtils.length(newLine.getContent()) == 0) {
                        oldLine.setContent("<span class='delete'>" + oldLine.getContent() + "</span>");
                    } else {
                        List<SegmentRange> oldSegmentRangeList = new ArrayList<>();
                        List<SegmentRange> newSegmentRangeList = new ArrayList<>();
                        List<SegmentPair> segmentPairList = LCSUtil.LCSCompare(oldLine.getContent(), newLine.getContent());
                        for (SegmentPair segmentpair : segmentPairList) {
                            oldSegmentRangeList.add(new SegmentRange(segmentpair.getOldBeginIndex(), segmentpair.getOldEndIndex(), segmentpair.isMatch()));
                            newSegmentRangeList.add(new SegmentRange(segmentpair.getNewBeginIndex(), segmentpair.getNewEndIndex(), segmentpair.isMatch()));
                        }
                        oldLine.setContent(LCSUtil.wrapChangePlace(oldLine.getContent(), oldSegmentRangeList, "<span class='delete'>", "</span>"));
                        newLine.setContent(LCSUtil.wrapChangePlace(newLine.getContent(), newSegmentRangeList, "<span class='insert'>", "</span>"));
                    }
                }
                oldResultList.add(oldLine);
                newResultList.add(newLine);
            } else {
                /** 修改多行，多行间需要做最优匹配 **/
                List<String> oldSubContentList = oldSubList.stream().map(AutoexecScriptLineVo::getContent).collect(Collectors.toList());
                List<String> newSubContentList = newSubList.stream().map(AutoexecScriptLineVo::getContent).collect(Collectors.toList());
                List<SegmentPair> segmentPairList = LCSUtil.differenceBestMatch(oldSubContentList, newSubContentList);
                for (SegmentPair segmentpair : segmentPairList) {
                    /** 递归 **/
                    regroupLineList(oldSubList, newSubList, oldResultList, newResultList, segmentpair);
                }
            }
        }
    }

    private AutoexecScriptLineVo createFillBlankLine(AutoexecScriptLineVo line) {
        AutoexecScriptLineVo fillBlankLine = new AutoexecScriptLineVo();
        fillBlankLine.setChangeType(ChangeType.FILLBLANK.getValue());
        fillBlankLine.setContent(line.getContent());
        return fillBlankLine;
    }

    /**
     * 将参数中的矩阵配置，转化成“矩阵名称.矩阵列名称”的格式，赋值到defaultValue中
     *
     * @param paramList
     */
    private void convertMatrixText(List<AutoexecScriptVersionParamVo> paramList) {
        if (CollectionUtils.isNotEmpty(paramList)) {
            for (AutoexecScriptVersionParamVo vo : paramList) {
                JSONObject config = vo.getConfig();
                if (MapUtils.isNotEmpty(config)) {
                    String matrixUuid = config.getString("matrixUuid");
                    String matrixValue = config.getString("matrixValue");
                    if (StringUtils.isNotBlank(matrixUuid) && StringUtils.isNotBlank(matrixValue)) {
                        MatrixVo matrix = matrixMapper.getMatrixByUuid(matrixUuid);
                        if (matrix != null) {
                            List<MatrixAttributeVo> attributeList = matrixAttributeMapper.getMatrixAttributeByMatrixUuid(matrixUuid);
                            Optional<MatrixAttributeVo> first = attributeList.stream().filter(o -> Objects.equals(o.getUuid(), matrixValue)).findFirst();
                            first.ifPresent(matrixAttributeVo -> vo.setDefaultValue(matrix.getName() + "." + matrixAttributeVo.getName()));
                        }
                    }
                }
            }
        }
    }


}
