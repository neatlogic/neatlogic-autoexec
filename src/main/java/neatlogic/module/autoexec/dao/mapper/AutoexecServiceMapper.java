/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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

    void updateServiceConfigExpiredById(AutoexecServiceVo serviceVo);

    void deleteServiceAuthorityListByServiceId(Long id);

    void deleteAutoexecServiceUserByServiceIdAndUserUuid(@Param("id") Long id, @Param("userUuid") String userUuid);

    void deleteAutoexecServiceById(Long id);

    void deleteAutoexecServiceUserByServiceId(Long id);

    void deleteAutoexecServiceAuthorityByServiceId(Long id);
}
