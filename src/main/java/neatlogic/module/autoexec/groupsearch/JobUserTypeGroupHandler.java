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
