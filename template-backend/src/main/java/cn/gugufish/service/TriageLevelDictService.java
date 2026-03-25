package cn.gugufish.service;

import cn.gugufish.entity.dto.TriageLevelDict;
import cn.gugufish.entity.vo.request.TriageLevelDictCreateVO;
import cn.gugufish.entity.vo.request.TriageLevelDictUpdateVO;

import java.util.List;

public interface TriageLevelDictService {
    List<TriageLevelDict> listTriageLevels();
    String createTriageLevel(TriageLevelDictCreateVO vo);
    String updateTriageLevel(TriageLevelDictUpdateVO vo);
    String deleteTriageLevel(int id);
}
