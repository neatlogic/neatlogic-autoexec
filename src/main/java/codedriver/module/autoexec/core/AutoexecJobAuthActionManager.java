/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.core;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
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
            if (JobStatus.PENDING.getValue().equalsIgnoreCase(jobVo.getStatus()) || jobVo.getPhaseList().stream().allMatch(o -> Objects.equals(o.getStatus(), JobPhaseStatus.PENDING.getValue()))) {
                jobVo.setIsCanJobFire(1);
            }
        });

        actionMap.put("pauseJob", (jobVo) -> {
            if (JobStatus.RUNNING.getValue().equalsIgnoreCase(jobVo.getStatus())) {
                jobVo.setIsCanJobPause(1);
            }
        });

        actionMap.put("abortJob", (jobVo) -> {
            if (JobStatus.RUNNING.getValue().equalsIgnoreCase(jobVo.getStatus())) {
                jobVo.setIsCanJobAbort(1);
            }
        });

        actionMap.put("goonJob", (jobVo) -> {
            if (JobStatus.ABORTED.getValue().equalsIgnoreCase(jobVo.getStatus()) || JobStatus.PAUSED.getValue().equalsIgnoreCase(jobVo.getStatus())) {
                jobVo.setIsCanJobGoon(1);
            }
        });

        actionMap.put("reFireJob", (jobVo) -> {
            if(CollectionUtils.isEmpty(jobVo.getPhaseList())){
                jobVo.setPhaseList(autoexecJobMapper.getJobPhaseListByJobId(jobVo.getId()));
            }
            if (jobVo.getPhaseList().stream().noneMatch(o -> Objects.equals(o.getStatus(), JobPhaseStatus.RUNNING.getValue())) && autoexecJobMapper.checkIsHasRunningNode(jobVo.getId()) == 0) {
                jobVo.setIsCanJobReFire(1);
            }
        });

        actionMap.put("resetJobNode", (jobVo) -> {
            if (JobStatus.ABORTED.getValue().equalsIgnoreCase(jobVo.getStatus()) || JobStatus.PAUSED.getValue().equalsIgnoreCase(jobVo.getStatus())) {
                jobVo.setIsCanJobNodeReset(1);
            }
        });

        actionMap.put("ignoreJobNode", (jobVo) -> {
            if (JobStatus.ABORTED.getValue().equalsIgnoreCase(jobVo.getStatus()) || JobStatus.PAUSED.getValue().equalsIgnoreCase(jobVo.getStatus())) {
                jobVo.setIsCanJobNodeIgnore(1);
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

        public Builder addAuthPauseJob() {
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

        public AutoexecJobAuthActionManager build() {
            return new AutoexecJobAuthActionManager(this);
        }
    }
}
