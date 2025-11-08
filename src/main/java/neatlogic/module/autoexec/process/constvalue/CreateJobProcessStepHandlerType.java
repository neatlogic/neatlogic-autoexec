/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.autoexec.process.constvalue;

import neatlogic.framework.process.stephandler.core.IProcessStepHandlerType;

/**
 * @author linbq
 * @since 2021/9/2 14:40
 **/
public enum CreateJobProcessStepHandlerType implements IProcessStepHandlerType {
    CREATE_JOB("createjob", "process", "自动化"),
    ;
    private String handler;
    private String name;
    private String type;

    CreateJobProcessStepHandlerType(String handler, String type, String name) {
        this.handler = handler;
        this.name = name;
        this.type = type;
    }
    @Override
    public String getHandler() {
        return handler;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }
}
