/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.autoexec.dao.mapper;

import codedriver.framework.autoexec.dto.AutoexecProxyGroupNetworkVo;
import codedriver.framework.autoexec.dto.AutoexecProxyGroupVo;
import codedriver.framework.autoexec.dto.AutoexecProxyVo;

import java.util.List;

public interface AutoexecProxyMapper {

    List<AutoexecProxyGroupNetworkVo> getAllNetworkMask();

    AutoexecProxyGroupVo getProxyGroupById(Long groupId);

    List<AutoexecProxyVo> getAllProxy();
}
