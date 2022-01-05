/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.service;

import codedriver.framework.autoexec.dao.mapper.AutoexecCatalogMapper;
import codedriver.framework.autoexec.dto.catalog.AutoexecCatalogVo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AutoexecCatalogServiceImpl implements AutoexecCatalogService {

    @Resource
    private AutoexecCatalogMapper autoexecCatalogMapper;

    @Override
    public AutoexecCatalogVo buildRootCatalog() {
        Integer maxRhtCode = autoexecCatalogMapper.getMaxRhtCode();
        AutoexecCatalogVo root = new AutoexecCatalogVo();
        root.setId(AutoexecCatalogVo.ROOT_ID);
        root.setName("所有");
        root.setParentId(AutoexecCatalogVo.ROOT_PARENTID);
        root.setLft(1);
        root.setRht(maxRhtCode == null ? 2 : maxRhtCode.intValue() + 1);
        return root;
    }
}
