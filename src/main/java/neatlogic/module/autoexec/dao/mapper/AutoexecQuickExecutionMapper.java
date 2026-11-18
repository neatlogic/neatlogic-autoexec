package neatlogic.module.autoexec.dao.mapper;

import neatlogic.framework.autoexec.dto.combop.AutoexecCombopVersionVo;
import neatlogic.framework.dto.AuthenticationInfoVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 快捷工具候选查询，只读取当前用户可执行的激活版本。 */
public interface AutoexecQuickExecutionMapper {
  /** 候选总数用于空集合判断，接口总数仍在配置准入过滤后计算。 */
  int countCandidates(
      @Param("auth") AuthenticationInfoVo auth,
      @Param("all") boolean all,
      @Param("keyword") String keyword,
      @Param("ids") List<Long> ids);

  /** 有界游标批量读取摘要与配置，授权多行不会重复返回工具。 */
  List<AutoexecCombopVersionVo> searchCandidates(
      @Param("auth") AuthenticationInfoVo auth,
      @Param("all") boolean all,
      @Param("keyword") String keyword,
      @Param("ids") List<Long> ids,
      @Param("beforeId") Long beforeId,
      @Param("size") int size);
}
