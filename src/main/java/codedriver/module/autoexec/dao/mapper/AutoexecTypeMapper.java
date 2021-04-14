/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.AutoexecTypeVo;

import java.util.List;

public interface AutoexecTypeMapper {

    public AutoexecTypeVo getTypeById(Long id);

    public int checkTypeNameIsExists(AutoexecTypeVo vo);

    public int searchTypeCount(AutoexecTypeVo vo);

    public List<AutoexecTypeVo> searchType(AutoexecTypeVo vo);

    public int checkTypeIsExistsById(Long id);

    /**
     * 检查插件类型是否被工具或脚本引用
     *
     * @param id 类型ID
     * @return 引用次数
     */
    public int checkTypeHasBeenReferredById(Long id);

    public int insertType(AutoexecTypeVo vo);

    public int updateType(AutoexecTypeVo vo);

    public int deleteTypeById(Long id);
}
