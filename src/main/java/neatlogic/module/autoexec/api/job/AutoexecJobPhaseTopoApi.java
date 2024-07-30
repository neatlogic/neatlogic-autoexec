/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.autoexec.api.job;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_BASE;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.AutoexecJobNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.graphviz.Graphviz;
import neatlogic.framework.graphviz.Layer;
import neatlogic.framework.graphviz.Link;
import neatlogic.framework.graphviz.Node;
import neatlogic.framework.graphviz.enums.LayoutType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.autoexec.service.AutoexecJobService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lvzk
 * @since 2022/5/6 11:20
 **/

@Service
@AuthAction(action = AUTOEXEC_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AutoexecJobPhaseTopoApi extends PrivateApiComponentBase {
    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getName() {
        return "获取作业阶段流程图";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业id", isRequired = true)
    })
    @Output({@Param(name = "topo", type = ApiParamType.STRING)})
    @Description(desc = "获取作业阶段流程图")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo == null) {
            throw new AutoexecJobNotFoundException(jobId);
        }
        List<AutoexecJobPhaseVo> phaseList = autoexecJobMapper.getJobPhaseListWithGroupByJobId(jobId);
        Map<Long, Layer.Builder> groupMap = new LinkedHashMap<>();
        if (CollectionUtils.isNotEmpty(phaseList)) {
            Graphviz.Builder gb = new Graphviz.Builder(LayoutType.get("dot")).withRankdir("LR");
            List<Long> groupIdList = new ArrayList<>();
            //获取分组列表
            for (AutoexecJobPhaseVo phase : phaseList) {
                if (!groupIdList.contains(phase.getGroupId())) {
                    groupIdList.add(phase.getGroupId());
                }
            }
            //输出分组图层
            for (int i = 0; i < groupIdList.size(); i++) {
                Long groupId = groupIdList.get(i);
                Layer.Builder phaseLayer = new Layer.Builder("Group" + groupId);
                phaseLayer.withLabel("");
                groupMap.put(groupId, phaseLayer);
                gb.addLayer(phaseLayer.build());

                if (i < groupIdList.size() - 1) {
                    //如果分组中的phase大于1，增加汇聚点
                    Layer.Builder convergeLayer = new Layer.Builder("Converge" + groupId);
                    convergeLayer.withLabel("");
                    Node.Builder nb = new Node.Builder("Converge_" + groupId);
                    nb.withLabel("")
                            .withHeight("0.2")
                            .withWidth("0.2")
                            .withShape("circle")
                            .addClass("converge");
                    convergeLayer.addNode(nb.build());
                    gb.addLayer(convergeLayer.build());
                }
            }
            for (AutoexecJobPhaseVo phase : phaseList) {
                int index = groupIdList.indexOf(phase.getGroupId());
                if (index > 0) {
                    Link.Builder linkBuilder = new Link.Builder("Converge_" + groupIdList.get(index - 1), "Phase_" + phase.getId());
                    gb.addLink(linkBuilder.build());
                }

                Layer.Builder lb = groupMap.get(phase.getGroupId());
                Node.Builder nb = new Node.Builder("Phase_" + phase.getId());
                nb.withTooltip(phase.getName())
                        .withLabel(phase.getName())
                        .withLabelloc("mc")
                        .withStyle("rounded")
                        .withShape("rect")
                        .withMargin("0.5,0.1")
                        .addClass("phasenode")
                        .addClass("phasenode-" + phase.getStatus());
                lb.addNode(nb.build());
                if (index < groupIdList.size() - 1) {
                    Link.Builder linkBuilder = new Link.Builder("Phase_" + phase.getId(), "Converge_" + phase.getGroupId());
                    gb.addLink(linkBuilder.build());
                }
            }
            Graphviz graphviz = gb.build();
            for (int i = graphviz.getNodeList().size() - 1; i >= 0; i--) {
                Node node = graphviz.getNodeList().get(i);
                if (node.getClassName().contains("converge")) {
                    //去掉只有一根连入连出线的汇聚节点
                    List<Link> incomingLinks = graphviz.getIncomingLinkById(node.getId());
                    List<Link> outgoingLinks = graphviz.getOutgoingLinkById(node.getId());
                    if (incomingLinks.size() == 1 && outgoingLinks.size() == 1) {
                        if (node.getLayer() != null) {
                            graphviz.removeLayer(node.getLayer().getId());
                        } else {
                            graphviz.removeNode(node.getId());
                        }
                        String fromPhase = incomingLinks.get(0).getFrom();
                        String toPhase = outgoingLinks.get(0).getTo();
                        Link.Builder linkBuilder = new Link.Builder(fromPhase, toPhase);
                        graphviz.addLink(linkBuilder.build());
                    }
                }
            }
            return graphviz.toString();
        }
        return null;
    }

    @Override
    public String getToken() {
        return "autoexec/job/phase/topo";
    }
}
