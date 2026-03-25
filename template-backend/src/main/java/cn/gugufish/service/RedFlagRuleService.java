package cn.gugufish.service;

import cn.gugufish.entity.vo.request.RedFlagRuleCreateVO;
import cn.gugufish.entity.vo.request.RedFlagRuleUpdateVO;
import cn.gugufish.entity.vo.response.RedFlagRuleVO;

import java.util.List;

public interface RedFlagRuleService {
    List<RedFlagRuleVO> listRules();
    String createRule(RedFlagRuleCreateVO vo);
    String updateRule(RedFlagRuleUpdateVO vo);
    String deleteRule(int id);
}
