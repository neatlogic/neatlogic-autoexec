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

package neatlogic.module.autoexec.process.dto;

import java.util.List;

public class CreateJobConfigMappingVo {
    private String mappingMode;
    private Object value;
    private String column;
    private List<CreateJobConfigFilterVo> filterList;
    private Boolean distinct;
    private List<Integer> limit;

    public String getMappingMode() {
        return mappingMode;
    }

    public void setMappingMode(String mappingMode) {
        this.mappingMode = mappingMode;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public List<CreateJobConfigFilterVo> getFilterList() {
        return filterList;
    }

    public void setFilterList(List<CreateJobConfigFilterVo> filterList) {
        this.filterList = filterList;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    public List<Integer> getLimit() {
        return limit;
    }

    public void setLimit(List<Integer> limit) {
        this.limit = limit;
    }
}
