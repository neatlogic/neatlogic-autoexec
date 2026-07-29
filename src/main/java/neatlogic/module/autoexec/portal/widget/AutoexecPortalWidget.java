/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package neatlogic.module.autoexec.portal.widget;

import neatlogic.framework.portal.widget.core.IPortalWidget;
import neatlogic.framework.portal.widget.core.IPortalWidgetGroup;

public enum AutoexecPortalWidget implements IPortalWidget {
    autoexecJobAttention("autoexecJobAttention", "执行中任务", 1, AutoexecPortalWidgetGroup.autoexecGroup1),
    autoexecFailedJob("autoexecFailedJob", "失败作业", 2, AutoexecPortalWidgetGroup.autoexecGroup1),
    autoexecPendingApproval("autoexecPendingApproval", "待审批任务", 3, AutoexecPortalWidgetGroup.autoexecGroup2),
    autoexecSuccessTrend("autoexecSuccessTrend", "近7日执行成功率", 4, AutoexecPortalWidgetGroup.autoexecGroup2),

    ;
    private final String value;
    private final String text;
    private final Integer sort;
    private final IPortalWidgetGroup group;

    AutoexecPortalWidget(String value, String text, Integer sort, IPortalWidgetGroup group) {
        this.value = value;
        this.text = text;
        this.sort = sort;
        this.group = group;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public String getText() {
        return this.text;
    }

    @Override
    public Integer getSort() {
        return this.sort;
    }

    @Override
    public IPortalWidgetGroup getGroup() {
        return this.group;
    }
}
