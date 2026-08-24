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

package neatlogic.module.autoexec.dto.job;

import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.EntityField;

public class AutoexecJobNodeOutputVo {

    @EntityField(name = "nmaaja.createautoexecjobfromcomboppublicapi.output.resourceid", type = ApiParamType.LONG)
    private Long resourceId;
    @EntityField(name = "nmaaja.createautoexecjobfromcomboppublicapi.output.host", type = ApiParamType.STRING)
    private String host;
    @EntityField(name = "nmaaja.createautoexecjobfromcomboppublicapi.output.port", type = ApiParamType.INTEGER)
    private Integer port;
    @EntityField(name = "nmaaja.createautoexecjobfromcomboppublicapi.output.output", type = ApiParamType.NOAUTH)
    private Object output;

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Object getOutput() {
        return output;
    }

    public void setOutput(Object output) {
        this.output = output;
    }
}
