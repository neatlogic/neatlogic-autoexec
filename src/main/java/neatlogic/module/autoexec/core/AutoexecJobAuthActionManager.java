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

package neatlogic.module.autoexec.core;

import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.exception.*;
import neatlogic.framework.dao.mapper.TeamMapper;
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
            //if (!Objects.equals(JobStatus.PENDING.getValue(),jobVo.getStatus()) && !jobVo.getPhaseList().stream().allMatch(o -> Objects.equals(o.getStatus(), JobPhaseStatus.PENDING.getValue()))) {
            //    throw new AutoexecJobCanNotFireException(jobVo.getId().toString());
            //}
        });

        actionMap.put("pauseJob", (jobVo) -> {
            if (!Objects.equals(JobStatus.RUNNING.getValue(),jobVo.getStatus())) {
                throw new AutoexecJobCanNotPauseException(jobVo.getId().toString());
            }
        });

        actionMap.put("abortJob", (jobVo) -> {
            /*if (!Arrays.asList(JobStatus.ABORTED.getValue(),JobStatus.COMPLETED.getValue(),JobStatus.FAILED.getValue(),JobStatus.PAUSED.getValue()).contains(jobVo.getStatus())) {
                throw new AutoexecJobCanNotAbortException(jobVo.getId().toString());
            }*/
        });

        actionMap.put("reFireJob", (jobVo) -> {
            /*if (Arrays.asList(JobStatus.RUNNING.getValue(),
                    JobStatus.PENDING.getValue(),JobStatus.ABORTING.getValue()).contains(jobVo.getStatus())) {
                throw new AutoexecJobCanNotRefireException(jobVo.getId().toString());
            }*/
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
