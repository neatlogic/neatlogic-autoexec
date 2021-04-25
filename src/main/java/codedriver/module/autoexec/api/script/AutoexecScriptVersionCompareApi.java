/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.api.script;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_MODIFY;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_REVIEW;
import codedriver.framework.autoexec.auth.AUTOEXEC_SCRIPT_USE;
import codedriver.framework.autoexec.constvalue.ChangeType;
import codedriver.framework.autoexec.dto.script.AutoexecScriptLineVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionParamVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.lcs.*;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.module.autoexec.service.AutoexecScriptService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = AUTOEXEC_SCRIPT_USE.class)
@AuthAction(action = AUTOEXEC_SCRIPT_MODIFY.class)
@AuthAction(action = AUTOEXEC_SCRIPT_REVIEW.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecScriptVersionCompareApi extends PrivateApiComponentBase {

    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecScriptService autoexecScriptService;

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

    @Input({
            @Param(name = "sourceVersionId", type = ApiParamType.LONG, isRequired = true, desc = "源版本ID"),
            @Param(name = "targetVersionId", type = ApiParamType.LONG, isRequired = true, desc = "目标版本ID"),
    })
    @Output({
            @Param(name = "sourceVersion", explode = AutoexecScriptVo[].class, desc = "当前版本脚本"),
            @Param(name = "targetVersion", explode = AutoexecScriptVo[].class, desc = "目标版本脚本"),
    })
    @Description(desc = "脚本版本对比")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject result = new JSONObject();
        Long sourceVersionId = jsonObj.getLong("sourceVersionId");
        Long targetVersionId = jsonObj.getLong("targetVersionId");
        AutoexecScriptVersionVo sourceVersion = autoexecScriptService.getScriptVersionDetailByVersionId(sourceVersionId);
        AutoexecScriptVersionVo targetVersion = autoexecScriptService.getScriptVersionDetailByVersionId(targetVersionId);
        result.put("sourceVersion", sourceVersion);
        result.put("targetVersion", targetVersion);
        if (!Objects.equals(sourceVersionId, targetVersionId)) {
            compareScriptVersion(targetVersion, sourceVersion);
        }
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
        // todo 参数对比，是否需要精确到字段域，比如判断究竟是参数名有变化，还是类型有变化
        compareParamList(sourceInputParamList, targetInputParamList);
        compareParamList(sourceOutputParamList, targetOutputParamList);
        if (!Objects.equals(source.getParser(), target.getParser())) {
            // todo 是否以插入html方式标识，待定
            source.setParser("<span class='update'>" + source.getParser() + "</span>");
            target.setParser("<span class='update'>" + target.getParser() + "</span>");
        }
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
                target.stream().forEach(o -> o.setChangeType(ChangeType.DELETE.getValue()));
            } else if (CollectionUtils.isEmpty(target)) {
                source.stream().forEach(o -> o.setChangeType(ChangeType.INSERT.getValue()));
            } else {
                // 对比行数相等部分的参数
                compareSameNumberParamList(source, target);
                if (source.size() > target.size()) {
                    // 将多余部分的参数标记为insert
                    for (int i = target.size(); i < source.size(); i++) {
                        source.get(i).setChangeType(ChangeType.INSERT.getValue());
                    }
                } else if (source.size() < target.size()) {
                    // 将多余部分的参数标记为delete
                    for (int i = source.size(); i < target.size(); i++) {
                        target.get(i).setChangeType(ChangeType.DELETE.getValue());
                    }
                }
            }
        } else {
            compareSameNumberParamList(source, target);
        }
    }

    /**
     * 以较短的参数列表的长度为准，对比行数相同的部分
     *
     * @param source
     * @param target
     */
    private void compareSameNumberParamList(List<AutoexecScriptVersionParamVo> source, List<AutoexecScriptVersionParamVo> target) {
        int size = source.size() <= target.size() ? source.size() : target.size();
        for (int i = 0; i < size; i++) {
            AutoexecScriptVersionParamVo beforeNextParam = source.get(i);
            AutoexecScriptVersionParamVo afterNextParam = target.get(i);
            if ((!Objects.equals(beforeNextParam.getKey(), afterNextParam.getKey()))
                    || (!Objects.equals(beforeNextParam.getDefaultValue(), afterNextParam.getDefaultValue()))
                    || (!Objects.equals(beforeNextParam.getType(), afterNextParam.getType()))
                    || (!Objects.equals(beforeNextParam.getMode(), afterNextParam.getMode()))
                    || (!Objects.equals(beforeNextParam.getIsRequired(), afterNextParam.getIsRequired()))
                    || (!Objects.equals(beforeNextParam.getDescription(), afterNextParam.getDescription()))
            ) {
                beforeNextParam.setChangeType(ChangeType.UPDATE.getValue());
                afterNextParam.setChangeType(ChangeType.UPDATE.getValue());
            }
        }
    }

    private void regroupLineList(List<AutoexecScriptLineVo> oldDataList, List<AutoexecScriptLineVo> newDataList, List<AutoexecScriptLineVo> oldResultList, List<AutoexecScriptLineVo> newResultList, SegmentPair segmentPair) {
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
                List<SegmentPair> segmentPairList = differenceBestMatch(oldSubList, newSubList);
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

    private List<SegmentPair> differenceBestMatch(List<AutoexecScriptLineVo> source, List<AutoexecScriptLineVo> target) {
        int sourceCount = source.size();
        int targetCount = target.size();
        NodePool nodePool = new NodePool(sourceCount, targetCount);
        for (int i = sourceCount - 1; i >= 0; i--) {
            for (int j = targetCount - 1; j >= 0; j--) {
                Node currentNode = new Node(i, j);
                AutoexecScriptLineVo oldLine = source.get(i);
                AutoexecScriptLineVo newLine = target.get(j);
                String oldContent = oldLine.getContent();
                String newContent = newLine.getContent();
                int oldLineContentLength = StringUtils.length(oldContent);
                int newLineContentLength = StringUtils.length(newContent);
                int minEditDistance = 0;
                if (oldLineContentLength > 0 && newLineContentLength > 0) {
                    minEditDistance = LCSUtil.minEditDistance(oldContent, newContent);
                } else {
                    minEditDistance = oldLineContentLength + newLineContentLength;
                }
                currentNode.setMinEditDistance(minEditDistance);
                int left = 0;
                int top = 0;
                int upperLeft = 0;
                Node upperLeftNode = nodePool.getOldNode(i + 1, j + 1);
                if (upperLeftNode != null) {
                    upperLeft = upperLeftNode.getTotalMatchLength();
                }
                Node leftNode = nodePool.getOldNode(i, j + 1);
                if (leftNode != null) {
                    left = leftNode.getTotalMatchLength();
                }
                Node topNode = nodePool.getOldNode(i + 1, j);
                if (topNode != null) {
                    top = topNode.getTotalMatchLength();
                }
                if (i + 1 == sourceCount && j + 1 == targetCount) {
                    currentNode.setTotalMatchLength(minEditDistance);
                } else if (i + 1 == sourceCount) {
                    currentNode.setTotalMatchLength(minEditDistance + left);
                    currentNode.setNext(leftNode);
                } else if (j + 1 == targetCount) {
                    currentNode.setTotalMatchLength(minEditDistance + top);
                    currentNode.setNext(topNode);
                } else {
                    if (upperLeft <= left) {
                        if (upperLeft <= top) {
                            currentNode.setTotalMatchLength(minEditDistance + upperLeft);
                            currentNode.setNext(upperLeftNode);
                        } else {
                            currentNode.setTotalMatchLength(minEditDistance + top);
                            currentNode.setNext(topNode);
                        }
                    } else if (top <= left) {
                        currentNode.setTotalMatchLength(minEditDistance + top);
                        currentNode.setNext(topNode);
                    } else {
                        currentNode.setTotalMatchLength(minEditDistance + left);
                        currentNode.setNext(leftNode);
                    }
                }

                nodePool.addNode(currentNode);
            }
        }
        List<Node> nodeList = new ArrayList<>();
        Node previous = null;
        Node node = nodePool.getOldNode(0, 0);
        while (node != null) {
            if (previous != null) {
                if (previous.getOldIndex() == node.getOldIndex() || previous.getNewIndex() == node.getNewIndex()) {
                    if (previous.getMinEditDistance() > node.getMinEditDistance()) {
                        previous = node;
                    }
                } else {
                    nodeList.add(previous);
                    previous = node;
                }
            } else {
                previous = node;
            }
            node = node.getNext();
        }
        if (previous != null) {
            nodeList.add(previous);
        }
        List<SegmentPair> segmentPairList = new ArrayList<>();
        int lastOldEndIndex = 0;
        int lastNewEndIndex = 0;
        for (Node n : nodeList) {
            if (n.getOldIndex() != lastOldEndIndex || n.getNewIndex() != lastNewEndIndex) {
                segmentPairList.add(new SegmentPair(lastOldEndIndex, n.getOldIndex(), lastNewEndIndex, n.getNewIndex(), false));
            }
            lastOldEndIndex = n.getOldIndex() + 1;
            lastNewEndIndex = n.getNewIndex() + 1;
            segmentPairList.add(new SegmentPair(n.getOldIndex(), lastOldEndIndex, n.getNewIndex(), lastNewEndIndex, false));
        }
        if (lastOldEndIndex != sourceCount || lastNewEndIndex != targetCount) {
            segmentPairList.add(new SegmentPair(lastOldEndIndex, sourceCount, lastNewEndIndex, targetCount, false));
        }
        return segmentPairList;
    }


}
