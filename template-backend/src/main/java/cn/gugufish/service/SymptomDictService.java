package cn.gugufish.service;

import cn.gugufish.entity.dto.SymptomDict;
import cn.gugufish.entity.vo.request.SymptomDictCreateVO;
import cn.gugufish.entity.vo.request.SymptomDictUpdateVO;

import java.util.List;

public interface SymptomDictService {
    List<SymptomDict> listSymptoms();
    String createSymptom(SymptomDictCreateVO vo);
    String updateSymptom(SymptomDictUpdateVO vo);
    String deleteSymptom(int id);
}
