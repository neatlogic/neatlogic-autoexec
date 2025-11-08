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

package neatlogic.module.autoexec.groupsearch;

import neatlogic.framework.autoexec.constvalue.JobGroupSearch;
import neatlogic.framework.autoexec.constvalue.JobUserType;
import neatlogic.framework.restful.groupsearch.core.GroupSearchOptionVo;
import neatlogic.framework.restful.groupsearch.core.GroupSearchVo;
import neatlogic.framework.restful.groupsearch.core.IGroupSearchHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class JobUserTypeGroupHandler implements IGroupSearchHandler {
    @Override
    public String getName() {
        return JobGroupSearch.JOBUSERTYPE.getValue();
    }

    @Override
    public String getLabel() {
        return JobGroupSearch.JOBUSERTYPE.getText();
    }

    @Override
    public String getHeader() {
        return getName() + "#";
    }

    @Override
    public List<GroupSearchOptionVo> search(GroupSearchVo groupSearchVo) {
        List<String> includeStrList = groupSearchVo.getIncludeList();
        if (CollectionUtils.isEmpty(includeStrList)) {
            includeStrList = new ArrayList<>();
        }
        List<String> excludeList = groupSearchVo.getExcludeList();
        List<String> valuelist = new ArrayList<>();
        List<GroupSearchOptionVo> userTypeList = new ArrayList<>();
        for (JobUserType s : JobUserType.values()) {
            if (s.getIsShow() && (StringUtils.isBlank(groupSearchVo.getKeyword()) || s.getText().contains(groupSearchVo.getKeyword()))) {
                String value = getHeader() + s.getValue();
                if (!valuelist.contains(value)) {
                    valuelist.add(value);
                    GroupSearchOptionVo groupSearchOptionVo = new GroupSearchOptionVo();
                    groupSearchOptionVo.setValue(value);
                    groupSearchOptionVo.setText(s.getText());
                    userTypeList.add(groupSearchOptionVo);
                }
            }
            if (includeStrList.contains(getHeader() + s.getValue())) {
                if (userTypeList.stream().noneMatch(o -> Objects.equals(o.getValue(), s.getValue()))) {
                    String value = getHeader() + s.getValue();
                    if (!valuelist.contains(value)) {
                        valuelist.add(value);
                        GroupSearchOptionVo groupSearchOptionVo = new GroupSearchOptionVo();
                        groupSearchOptionVo.setValue(value);
                        groupSearchOptionVo.setText(s.getText());
                        userTypeList.add(groupSearchOptionVo);
                    }
                }
            }
        }
        return userTypeList;
    }

    @Override
    public List<GroupSearchOptionVo> reload(GroupSearchVo groupSearchVo) {
        List<GroupSearchOptionVo> userTypeList = new ArrayList<>();
        List<String> valueList = groupSearchVo.getValueList();
        if (CollectionUtils.isNotEmpty(valueList)) {
            for (String value : valueList) {
                if (value.startsWith(getHeader())) {
                    String realValue = value.replace(getHeader(), "");
                    String text = JobUserType.getText(realValue);
                    if (StringUtils.isNotBlank(text)) {
                        GroupSearchOptionVo groupSearchOptionVo = new GroupSearchOptionVo();
                        groupSearchOptionVo.setValue(value);
                        groupSearchOptionVo.setText(text);
                        userTypeList.add(groupSearchOptionVo);
                    }
                }
            }
        }
        return userTypeList;
    }

    @Override
    public int getSort() {
        return 1;
    }

    @Override
    public Boolean isLimit() {
        return false;
    }
}
