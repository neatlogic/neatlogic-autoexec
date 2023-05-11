/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.api.script;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_MANAGE;
import neatlogic.framework.autoexec.auth.AUTOEXEC_SCRIPT_SEARCH;
import neatlogic.framework.autoexec.constvalue.ScriptParser;
import neatlogic.framework.autoexec.constvalue.ScriptVersionStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import neatlogic.framework.autoexec.dto.AutoexecParamConfigVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptArgumentVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptLineVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import neatlogic.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dao.mapper.RoleMapper;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.RoleVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.dto.WorkAssignmentUnitVo;
import neatlogic.framework.file.dao.mapper.FileMapper;
import neatlogic.framework.file.dto.FileVo;
import neatlogic.framework.lcs.BaseLineVo;
import neatlogic.framework.lcs.LCSUtil;
import neatlogic.framework.lcs.SegmentPair;
import neatlogic.framework.lcs.constvalue.ChangeType;
import neatlogic.framework.lcs.constvalue.LineHandler;
import neatlogic.framework.matrix.dao.mapper.MatrixAttributeMapper;
import neatlogic.framework.matrix.dao.mapper.MatrixMapper;
import neatlogic.framework.matrix.dto.MatrixAttributeVo;
import neatlogic.framework.matrix.dto.MatrixVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecScriptService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
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

    @Resource
    private FileMapper fileMapper;

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
     * @param target 目标版本
     * @param source 源版本
     */
    private void compareScriptVersion(AutoexecScriptVersionVo target, AutoexecScriptVersionVo source) {
        List<AutoexecScriptVersionParamVo> targetInputParamList = target.getInputParamList() != null ? target.getInputParamList() : new ArrayList<>();
        List<AutoexecScriptVersionParamVo> targetOutputParamList = target.getOutputParamList() != null ? target.getOutputParamList() : new ArrayList<>();
        List<AutoexecScriptVersionParamVo> sourceInputParamList = source.getInputParamList() != null ? source.getInputParamList() : new ArrayList<>();
        List<AutoexecScriptVersionParamVo> sourceOutputParamList = source.getOutputParamList() != null ? source.getOutputParamList() : new ArrayList<>();
        compareParamList(sourceInputParamList, targetInputParamList);
        compareParamList(sourceOutputParamList, targetOutputParamList);
        compareArgument(target.getArgument(), source.getArgument());
        if (!StringUtils.equals(target.getParser(), ScriptParser.PACKAGE.getValue()) && !StringUtils.equals(source.getParser(), ScriptParser.PACKAGE.getValue())) {
            compareLineList(target, source);
        } else if (StringUtils.equals(target.getParser(), ScriptParser.PACKAGE.getValue()) && StringUtils.equals(source.getParser(), ScriptParser.PACKAGE.getValue())) {
            FileVo targetPackageFile = target.getPackageFile();
            FileVo sourcePackageFile = source.getPackageFile();
            if (targetPackageFile != null && sourcePackageFile != null && !Objects.equals(targetPackageFile.getId(), sourcePackageFile.getId())) {
                targetPackageFile.setName("<span class='update'>" + targetPackageFile.getName() + "</span>");
                sourcePackageFile.setName("<span class='update'>" + sourcePackageFile.getName() + "</span>");
            }

        }
        if (!Objects.equals(target.getParser(), source.getParser())) {
            target.setParser("<span class='update'>" + target.getParser() + "</span>");
            source.setParser("<span class='update'>" + source.getParser() + "</span>");
        }
        compareUseLibName(source, target);
    }

    /**
     * 依赖工具对比
     *
     * @param source 来源版本
     * @param target 目标版本
     */
    private void compareUseLibName(AutoexecScriptVersionVo source, AutoexecScriptVersionVo target) {
        List<String> sourceUseLibNameList = source.getUseLibName();
        List<String> targetUseLibNameList = target.getUseLibName();
        if (CollectionUtils.isNotEmpty(sourceUseLibNameList) && CollectionUtils.isEmpty(targetUseLibNameList)) {
            source.setUseLibName(sourceUseLibNameList.stream().map(e -> ("<span class='insert'>" + e + "</span>")).collect(Collectors.toList()));
        } else if (CollectionUtils.isEmpty(sourceUseLibNameList) && CollectionUtils.isNotEmpty(targetUseLibNameList)) {
            target.setUseLibName(targetUseLibNameList.stream().map(e -> "<span class='delete'>" + e + "</span>").collect(Collectors.toList()));
        } else if (CollectionUtils.isNotEmpty(sourceUseLibNameList) && CollectionUtils.isNotEmpty(targetUseLibNameList)) {
            List<String> sourceNewUseLibNameList = sourceUseLibNameList.stream().filter(item -> !targetUseLibNameList.contains(item)).collect(Collectors.toList());
            List<String> targetDeleteUseLibNameList = targetUseLibNameList.stream().filter(item -> !sourceUseLibNameList.contains(item)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(sourceNewUseLibNameList)) {
                source.setUseLibName(sourceUseLibNameList.stream().map(e -> sourceNewUseLibNameList.contains(e) ? ("<span class='insert'>" + e + "</span>") : e).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(targetDeleteUseLibNameList)) {
                target.setUseLibName(targetUseLibNameList.stream().map(e -> targetDeleteUseLibNameList.contains(e) ? ("<span class='delete'>" + e + "</span>") : e).collect(Collectors.toList()));
            }
        }
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
     * 对比自由参数
     *
     * @param source
     * @param target
     */
    private void compareArgument(AutoexecScriptArgumentVo source, AutoexecScriptArgumentVo target) {
        if (source == null && target == null) {
            return;
        }
        if (source == null) {
            target.setChangeType(ChangeType.DELETE.getValue());
        } else if (target == null) {
            source.setChangeType(ChangeType.INSERT.getValue());
        } else if (!Objects.equals(source, target)) {
            source.setChangeType(ChangeType.UPDATE.getValue());
            target.setChangeType(ChangeType.UPDATE.getValue());
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
        List<BaseLineVo> sourceResultList = new ArrayList<>();
        List<BaseLineVo> targetResultList = new ArrayList<>();
        List<BaseLineVo> sourceBaseLineList = autoexecScriptLineVoListConvertBaseLineVoList(sourceLineList);
        List<BaseLineVo> targetBaseLineList = autoexecScriptLineVoListConvertBaseLineVoList(targetLineList);

        List<SegmentPair> segmentPairList = LCSUtil.LCSCompare(sourceBaseLineList, targetBaseLineList);
        for (SegmentPair segmentPair : segmentPairList) {
            LCSUtil.regroupLineList(
                    sourceBaseLineList,
                    targetBaseLineList,
                    sourceResultList,
                    targetResultList,
                    segmentPair);
        }
        source.setLineList(baseLineVoListConvertAutoexecScriptLineVoList(sourceResultList));
        target.setLineList(baseLineVoListConvertAutoexecScriptLineVoList(targetResultList));
    }

    /**
     * 将AutoexecScriptLineVo列表转换成BaseLineVo列表
     *
     * @param lineList
     * @return
     */
    private List<BaseLineVo> autoexecScriptLineVoListConvertBaseLineVoList(List<AutoexecScriptLineVo> lineList) {
        List<BaseLineVo> resultList = new ArrayList<>();
        for (AutoexecScriptLineVo lineVo : lineList) {
            lineVo.setHandler(LineHandler.TEXT.getValue());
            resultList.add(lineVo);
        }
        return resultList;
    }

    /**
     * 将BaseLineVo列表转换成AutoexecScriptLineVo列表
     *
     * @param lineList
     * @return
     */
    private List<AutoexecScriptLineVo> baseLineVoListConvertAutoexecScriptLineVoList(List<BaseLineVo> lineList) {
        List<AutoexecScriptLineVo> resultList = new ArrayList<>();
        for (BaseLineVo lineVo : lineList) {
            if (lineVo instanceof AutoexecScriptLineVo) {
                resultList.add((AutoexecScriptLineVo) lineVo);
            }
        }
        return resultList;
    }

//    private void regroupLineList(List<AutoexecScriptLineVo> oldDataList, List<AutoexecScriptLineVo> newDataList
//            , List<AutoexecScriptLineVo> oldResultList, List<AutoexecScriptLineVo> newResultList, SegmentPair segmentPair) {
//        List<AutoexecScriptLineVo> oldSubList = oldDataList.subList(segmentPair.getOldBeginIndex(), segmentPair.getOldEndIndex());
//        List<AutoexecScriptLineVo> newSubList = newDataList.subList(segmentPair.getNewBeginIndex(), segmentPair.getNewEndIndex());
//        if (segmentPair.isMatch()) {
//            /** 分段对匹配时，行数据不能做标记，直接添加到重组后的数据列表中 **/
//            oldResultList.addAll(oldSubList);
//            newResultList.addAll(newSubList);
//        } else {
//            /** 分段对不匹配时，分成下列四种情况 **/
//            if (CollectionUtils.isEmpty(newSubList)) {
//                /** 删除行 **/
//                for (AutoexecScriptLineVo lineVo : oldSubList) {
//                    lineVo.setChangeType(ChangeType.DELETE.getValue());
//                    oldResultList.add(lineVo);
//                    newResultList.add(createFillBlankLine(lineVo));
//                }
//            } else if (CollectionUtils.isEmpty(oldSubList)) {
//                /** 插入行 **/
//                for (AutoexecScriptLineVo lineVo : newSubList) {
//                    oldResultList.add(createFillBlankLine(lineVo));
//                    lineVo.setChangeType(ChangeType.INSERT.getValue());
//                    newResultList.add(lineVo);
//                }
//            } else if (oldSubList.size() == 1 && newSubList.size() == 1) {
//                /** 修改一行 **/
//                AutoexecScriptLineVo oldLine = oldSubList.get(0);
//                AutoexecScriptLineVo newLine = newSubList.get(0);
//                if (!Objects.equals(oldLine.getContent(), newLine.getContent())) {
//                    oldLine.setChangeType(ChangeType.UPDATE.getValue());
//                    newLine.setChangeType(ChangeType.UPDATE.getValue());
//                    if (StringUtils.length(oldLine.getContent()) == 0) {
//                        newLine.setContent("<span class='insert'>" + newLine.getContent() + "</span>");
//                    } else if (StringUtils.length(newLine.getContent()) == 0) {
//                        oldLine.setContent("<span class='delete'>" + oldLine.getContent() + "</span>");
//                    } else {
//                        List<SegmentRange> oldSegmentRangeList = new ArrayList<>();
//                        List<SegmentRange> newSegmentRangeList = new ArrayList<>();
//                        List<SegmentPair> segmentPairList = LCSUtil.LCSCompare(oldLine.getContent(), newLine.getContent());
//                        for (SegmentPair segmentpair : segmentPairList) {
//                            oldSegmentRangeList.add(new SegmentRange(segmentpair.getOldBeginIndex(), segmentpair.getOldEndIndex(), segmentpair.isMatch()));
//                            newSegmentRangeList.add(new SegmentRange(segmentpair.getNewBeginIndex(), segmentpair.getNewEndIndex(), segmentpair.isMatch()));
//                        }
//                        oldLine.setContent(LCSUtil.wrapChangePlace(oldLine.getContent(), oldSegmentRangeList, "<span class='delete'>", "</span>"));
//                        newLine.setContent(LCSUtil.wrapChangePlace(newLine.getContent(), newSegmentRangeList, "<span class='insert'>", "</span>"));
//                    }
//                }
//                oldResultList.add(oldLine);
//                newResultList.add(newLine);
//            } else {
//                /** 修改多行，多行间需要做最优匹配 **/
//                List<String> oldSubContentList = oldSubList.stream().map(AutoexecScriptLineVo::getContent).collect(Collectors.toList());
//                List<String> newSubContentList = newSubList.stream().map(AutoexecScriptLineVo::getContent).collect(Collectors.toList());
//                List<SegmentPair> segmentPairList = LCSUtil.differenceBestMatch(oldSubContentList, newSubContentList);
//                for (SegmentPair segmentpair : segmentPairList) {
//                    /** 递归 **/
//                    regroupLineList(oldSubList, newSubList, oldResultList, newResultList, segmentpair);
//                }
//            }
//        }
//    }

//    private AutoexecScriptLineVo createFillBlankLine(AutoexecScriptLineVo line) {
//        AutoexecScriptLineVo fillBlankLine = new AutoexecScriptLineVo();
//        fillBlankLine.setChangeType(ChangeType.FILLBLANK.getValue());
//        fillBlankLine.setContent(line.getContent());
//        return fillBlankLine;
//    }

    /**
     * 将参数中的矩阵配置，转化成“矩阵名称.矩阵列名称”的格式，赋值到defaultValue中
     *
     * @param paramList
     */
    private void convertMatrixText(List<AutoexecScriptVersionParamVo> paramList) {
        if (CollectionUtils.isNotEmpty(paramList)) {
            for (AutoexecScriptVersionParamVo vo : paramList) {
                AutoexecParamConfigVo config = vo.getConfig();
                if (config != null) {
                    String matrixUuid = config.getMatrixUuid();
                    String text = "";
                    JSONObject mapping = config.getMapping();
                    if (MapUtils.isNotEmpty(mapping)) {
                        text = mapping.getString("text");
                    }
                    String textColumnUuid = text;
                    if (StringUtils.isNotBlank(matrixUuid) && StringUtils.isNotBlank(textColumnUuid)) {
                        MatrixVo matrix = matrixMapper.getMatrixByUuid(matrixUuid);
                        if (matrix != null) {
                            List<MatrixAttributeVo> attributeList = matrixAttributeMapper.getMatrixAttributeByMatrixUuid(matrixUuid);
                            Optional<MatrixAttributeVo> first = attributeList.stream().filter(o -> Objects.equals(o.getUuid(), textColumnUuid)).findFirst();
                            first.ifPresent(matrixAttributeVo -> vo.setDefaultValue(matrix.getName() + "." + matrixAttributeVo.getName()));
                        }
                    }
                }
            }
        }
    }


}
