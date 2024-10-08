/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
