/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.core;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.exception.*;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @author lvzkR
 * @since 2021/4/14 11:12
 **/
@Component
public class AutoexecJobAuthActionManager {
    private static TeamMapper teamMapper;
    private static AutoexecJobMapper autoexecJobMapper;
    public static final Map<String, Action<AutoexecJobVo>> actionMap = new HashMap<>();
    private List<String> actionList = new ArrayList<>();

    public AutoexecJobAuthActionManager() {
    }

    @Autowired
    public AutoexecJobAuthActionManager(TeamMapper _teamMapper, AutoexecJobMapper _autoexecJobMapper) {
        teamMapper = _teamMapper;
        autoexecJobMapper = _autoexecJobMapper;
    }

    public AutoexecJobAuthActionManager(Builder builder) {
        actionList = builder.actionList;
    }

    @PostConstruct
    public void actionDispatcherInit() {
        actionMap.put("fireJob", (jobVo) -> {
            if (!Objects.equals(JobStatus.PENDING.getValue(),jobVo.getStatus()) && !jobVo.getPhaseList().stream().allMatch(o -> Objects.equals(o.getStatus(), JobPhaseStatus.PENDING.getValue()))) {
                throw new AutoexecJobCanNotFireException(jobVo.getId().toString());
            }
        });

        actionMap.put("pauseJob", (jobVo) -> {
            if (!Objects.equals(JobStatus.RUNNING.getValue(),jobVo.getStatus())) {
                throw new AutoexecJobCanNotPauseException(jobVo.getId().toString());
            }
        });

        actionMap.put("abortJob", (jobVo) -> {
            if (!Objects.equals(JobStatus.RUNNING.getValue(),jobVo.getStatus())) {
                throw new AutoexecJobCanNotAbortException(jobVo.getId().toString());
            }
        });

        actionMap.put("goonJob", (jobVo) -> {
            if (!Objects.equals(JobStatus.ABORTED.getValue(),jobVo.getStatus()) && !Objects.equals(JobStatus.PAUSED.getValue(),jobVo.getStatus())) {
                throw new AutoexecJobCanNotGoonException(jobVo.getId().toString());
            }
        });

        actionMap.put("reFireJob", (jobVo) -> {
            if(CollectionUtils.isEmpty(jobVo.getPhaseList())){
                jobVo.setPhaseList(autoexecJobMapper.getJobPhaseListByJobId(jobVo.getId()));
            }
            if (jobVo.getPhaseList().stream().anyMatch(o -> Objects.equals(o.getStatus(), JobPhaseStatus.RUNNING.getValue())) || autoexecJobMapper.checkIsHasRunningNode(jobVo.getId()) != 0) {
                throw new AutoexecJobCanNotRefireException(jobVo.getId().toString());
            }
        });

        actionMap.put("resetJobNode", (jobVo) -> {
        });

        actionMap.put("ignoreJobNode", (jobVo) -> {
            if (!Objects.equals(JobStatus.ABORTED.getValue(),jobVo.getStatus()) && !Objects.equals(JobStatus.PAUSED.getValue(),jobVo.getStatus()) && !Objects.equals(JobStatus.COMPLETED.getValue(),jobVo.getStatus())) {
                throw new AutoexecJobCanNotIgnoreJobNodeException(jobVo.getNodeId().toString());
            }
        });

        actionMap.put("refireJobNode", (jobVo) -> {
            if (!Objects.equals(JobStatus.ABORTED.getValue(),jobVo.getStatus()) && !Objects.equals(JobStatus.PAUSED.getValue(),jobVo.getStatus()) && !Objects.equals(JobStatus.COMPLETED.getValue(),jobVo.getStatus())) {
                throw new AutoexecJobCanNotRefireJobNodeException(jobVo.getNodeId().toString());
            }
        });
    }


    @FunctionalInterface
    public interface Action<T> {
        void execute(T t);
    }

    /**
     * 设置作业操作权限
     * 1、先判断有没有执行权限
     * 2、按需要，根据指定操作判断权限（如果没有指定的，默认判断所有操作权限）
     *
     * @param autoexecJobVo 作业参数
     */
    public void setAutoexecJobAction(AutoexecJobVo autoexecJobVo) {
        List<String> userList = new ArrayList<>();
        userList.add(UserContext.get().getUserUuid());
        userList.addAll(UserContext.get().getRoleUuidList());
        userList.addAll(teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid()));
        if (autoexecJobMapper.checkIsJobUser(autoexecJobVo.getId(), userList) > 0) {
            if (CollectionUtils.isNotEmpty(actionList)) {
                for (String action : actionList) {
                    actionMap.get(action).execute(autoexecJobVo);
                }
            } else {
                for (Map.Entry<String, Action<AutoexecJobVo>> entry : actionMap.entrySet()) {
                    entry.getValue().execute(autoexecJobVo);
                }
            }
        }
    }

    /**
     * 构建
     */
    public static class Builder {
        private final List<String> actionList = new ArrayList<>();

        public Builder addPauseJob() {
            this.actionList.add("pauseJob");
            return this;
        }

        public Builder addAbortJob() {
            this.actionList.add("abortJob");
            return this;
        }

        public Builder addGoonJob() {
            this.actionList.add("goonJob");
            return this;
        }

        public Builder addFireJob() {
            this.actionList.add("fireJob");
            return this;
        }

        public Builder addReFireJob() {
            this.actionList.add("reFireJob");
            return this;
        }

        public Builder addResetJobNode() {
            this.actionList.add("resetJobNode");
            return this;
        }

        public AutoexecJobAuthActionManager build() {
            return new AutoexecJobAuthActionManager(this);
        }
    }
}
