/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.autoexec.service;

import neatlogic.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import neatlogic.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import neatlogic.framework.autoexec.exception.AutoexecCatalogNotFoundException;
import neatlogic.framework.lrcode.LRCodeManager;
import neatlogic.framework.lrcode.dao.mapper.TreeMapper;
import neatlogic.framework.lrcode.dto.TreeNodeVo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AutoexecCatalogServiceImpl implements AutoexecCatalogService {

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Resource
    private TreeMapper treeMapper;

    @Override
    public AutoexecCatalogVo buildRootCatalog() {
        Integer maxRhtCode = autoexecCatalogMapper.getMaxRhtCode();
        AutoexecCatalogVo root = new AutoexecCatalogVo();
        root.setId(AutoexecCatalogVo.ROOT_ID);
        root.setName(AutoexecCatalogVo.ROOT_NAME);
        root.setParentId(AutoexecCatalogVo.ROOT_PARENTID);
        root.setLft(1);
        root.setRht(maxRhtCode == null ? 2 : maxRhtCode.intValue() + 1);
        return root;
    }

    @Override
    public Long saveAutoexecCatalog(AutoexecCatalogVo autoexecCatalogVo) {
        if (autoexecCatalogMapper.checkAutoexecCatalogIsExists(autoexecCatalogVo.getId()) > 0) {
            autoexecCatalogMapper.updateAutoexecCatalogNameById(autoexecCatalogVo);
        } else {
            if (!AutoexecCatalogVo.ROOT_ID.equals(autoexecCatalogVo.getParentId()) && autoexecCatalogMapper.checkAutoexecCatalogIsExists(autoexecCatalogVo.getParentId()) == 0) {
                throw new AutoexecCatalogNotFoundException(autoexecCatalogVo.getParentId());
            }
            int lft = LRCodeManager.beforeAddTreeNode("autoexec_catalog", "id", "parent_id", autoexecCatalogVo.getParentId());
            autoexecCatalogVo.setLft(lft);
            autoexecCatalogVo.setRht(lft + 1);
            List<Long> upwardRegionIdList = new ArrayList<>();
            List<String> upwardRegionNameList = new ArrayList<>();
            TreeNodeVo treeNodeVo = new TreeNodeVo("autoexec_catalog", "id", "name","parent_id", autoexecCatalogVo.getLft(), autoexecCatalogVo.getRht());
            List<TreeNodeVo> upwardRegionList = treeMapper.getAncestorsAndSelfByLftRht(treeNodeVo);
            for (TreeNodeVo upwardRegion : upwardRegionList) {
                upwardRegionIdList.add(Long.parseLong(upwardRegion.getIdKey()));
                upwardRegionNameList.add(upwardRegion.getNameKey());
            }
            upwardRegionIdList.add(autoexecCatalogVo.getId());
            upwardRegionNameList.add(autoexecCatalogVo.getName());
            autoexecCatalogVo.setUpwardIdPath(upwardRegionIdList.stream().map(Object::toString).collect(Collectors.joining(",")));
            autoexecCatalogVo.setUpwardNamePath(String.join("/", upwardRegionNameList));
            autoexecCatalogMapper.insertAutoexecCatalog(autoexecCatalogVo);
        }
        return autoexecCatalogVo.getId();
    }
}
