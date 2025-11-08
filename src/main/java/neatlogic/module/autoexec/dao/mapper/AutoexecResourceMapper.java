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

package neatlogic.module.autoexec.dao.mapper;

import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.SoftwareServiceOSVo;

import java.util.List;

public interface AutoexecResourceMapper {

    List<SoftwareServiceOSVo> getOsResourceListByResourceIdList(List<Long> resourceIdList);

    List<ResourceVo> getOsResourceListenPortListByResourceIdList(List<Long> resourceIdList);

    List<Long> getOsResourceIdListByAppSystemIdAndAppModuleIdAndEnvIdAndTypeId(ResourceSearchVo searchVo);

    int getOsResourceCountByAppSystemIdAndAppModuleIdListAndEnvIdAndTypeId(ResourceSearchVo searchVo);

    List<ResourceVo> getResourceAppSystemListByResourceIdList(List<Long> id);

    List<ResourceVo> getSoftwareResourceListenPortListByResourceIdList(List<Long> resourceIdList);
}
