/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.core;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.module.autoexec.dao.mapper.AutoexecJobMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lvzkR
 * @since 2021/4/14 11:12
 **/
@Component
public class AutoexecJobAuthActionManager {
    private static TeamMapper teamMapper;
    private static AutoexecJobMapper autoexecJobMapper;
    public final Map<String, Action<AutoexecJobVo>> actionMap = new HashMap<>();
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
        actionMap.put("pauseJob", (jobVo) -> {
            if (JobStatus.RUNNING.getValue().equalsIgnoreCase(jobVo.getStatus())) {
                jobVo.setIsCanJobPause(1);
            }
        });

        actionMap.put("stopJob", (jobVo) -> {
            if (JobStatus.RUNNING.getValue().equalsIgnoreCase(jobVo.getStatus())) {
                jobVo.setIsCanJobStop(1);
            }
        });

        actionMap.put("goonJob", (jobVo) -> {
            if (JobStatus.STOPPED.getValue().equalsIgnoreCase(jobVo.getStatus()) || JobStatus.PAUSED.getValue().equalsIgnoreCase(jobVo.getStatus())) {
                jobVo.setIsCanJobGoon(1);
            }
        });

        actionMap.put("redoJob", (jobVo) -> {
            if (JobStatus.STOPPED.getValue().equalsIgnoreCase(jobVo.getStatus()) || JobStatus.PAUSED.getValue().equalsIgnoreCase(jobVo.getStatus())) {
                jobVo.setIsCanJobRedo(1);
            }
        });

        actionMap.put("resetJobNode", (jobVo) -> {
            if (JobStatus.STOPPED.getValue().equalsIgnoreCase(jobVo.getStatus()) || JobStatus.PAUSED.getValue().equalsIgnoreCase(jobVo.getStatus())) {
                jobVo.setIsCanJobNodeReset(1);
            }
        });

        actionMap.put("ignoreJobNode", (jobVo) -> {
            if (JobStatus.STOPPED.getValue().equalsIgnoreCase(jobVo.getStatus()) || JobStatus.PAUSED.getValue().equalsIgnoreCase(jobVo.getStatus())) {
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
     * @param autoexecJobVo 作业参数
     */
    public void setAutoexecJobAction(AutoexecJobVo autoexecJobVo) {
        List<String> userList = new ArrayList<>();
        userList.add(UserContext.get().getUserUuid());
        userList.addAll(UserContext.get().getRoleUuidList());
        userList.addAll(teamMapper.getTeamUuidListByUserUuid(UserContext.get().getUserUuid()));
        if (autoexecJobMapper.checkIsAutoexecJobUser(autoexecJobVo.getId(), userList) > 0) {
            if(CollectionUtils.isNotEmpty(actionList)) {
                for (String action : actionList) {
                    actionMap.get(action).execute(autoexecJobVo);
                }
            }else{
                for(Map.Entry<String,Action<AutoexecJobVo>> entry : actionMap.entrySet()){
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

        public Builder addStopJob() {
            this.actionList.add("stopJob");
            return this;
        }

        public Builder addGoonJob() {
            this.actionList.add("goonJob");
            return this;
        }

        public Builder addRedoJob() {
            this.actionList.add("redoJob");
            return this;
        }

        public Builder addResetJobNode() {
            this.actionList.add("resetJobNode");
            return this;
        }

        public Builder addIgnoreJobNode() {
            this.actionList.add("ignoreJobNode");
            return this;
        }

        public AutoexecJobAuthActionManager build() {
            return new AutoexecJobAuthActionManager(this);
        }
    }
}
