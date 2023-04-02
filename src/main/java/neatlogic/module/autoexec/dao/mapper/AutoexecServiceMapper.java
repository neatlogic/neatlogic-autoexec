/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.autoexec.dao.mapper;

import neatlogic.framework.autoexec.dto.service.AutoexecServiceAuthorityVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceNodeVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceSearchVo;
import neatlogic.framework.autoexec.dto.service.AutoexecServiceVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AutoexecServiceMapper {

    AutoexecServiceVo getAutoexecServiceById(Long id);

    List<AutoexecServiceVo> getAutoexecServiceListByIdList(List<Long> idList);

    AutoexecServiceNodeVo getAutoexecServiceNodeById(Long id);

    int checkAutoexecServiceNameIsRepeat(AutoexecServiceVo vo);

    int getAllCount();

    List<AutoexecServiceNodeVo> getAutoexecServiceNodeList(AutoexecServiceSearchVo searchVo);

    int searchAutoexecServiceCount(AutoexecServiceSearchVo searchVo);

    List<AutoexecServiceVo> searchAutoexecServiceList(AutoexecServiceSearchVo searchVo);

    int getAllVisibleCount(AutoexecServiceSearchVo searchVo);

    List<AutoexecServiceNodeVo> getAutoexecServiceNodeVisibleList(AutoexecServiceSearchVo searchVo);

    int getAutoexecServiceUserCount(AutoexecServiceSearchVo searchVo);

    List<AutoexecServiceVo> getAutoexecServiceUserList(AutoexecServiceSearchVo searchVo);

    List<AutoexecServiceAuthorityVo> getAutoexecServiceAuthorityListByServiceId(Long id);

    List<String> getUpwardNameListByLftAndRht(@Param("lft") Integer lft, @Param("rht") Integer rht);

    List<Long> getUpwardIdListByLftAndRht(@Param("lft") Integer lft, @Param("rht") Integer rht);

    List<Long> getFavoriteAutoexecServiceIdListByUserUuidAndServiceIdList(@Param("userUuid") String userUuid, @Param("serviceIdList") List<Long> serviceIdList);

    int getAutoexecServiceCountByParentId(Long parentId);

    void insertAutoexecService(AutoexecServiceVo serviceVo);

    void insertAutoexecServiceConfig(AutoexecServiceVo serviceVo);

    void insertAutoexecServiceAuthorityList(List<AutoexecServiceAuthorityVo> authorityList);

    void insertAutoexecServiceUser(@Param("id") Long id, @Param("userUuid") String userUuid);

    void updateServiceById(AutoexecServiceVo serviceVo);

    void updateServiceIsActiveById(AutoexecServiceVo searchVo);

    void deleteServiceAuthorityListByServiceId(Long id);

    void deleteAutoexecServiceUserByServiceIdAndUserUuid(@Param("id") Long id, @Param("userUuid") String userUuid);

    void deleteAutoexecServiceById(Long id);

    void deleteAutoexecServiceUserByServiceId(Long id);

    void deleteAutoexecServiceAuthorityByServiceId(Long id);
}
