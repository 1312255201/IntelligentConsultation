package cn.gugufish.service;

import cn.gugufish.entity.dto.BodyPartDict;
import cn.gugufish.entity.vo.request.BodyPartDictCreateVO;
import cn.gugufish.entity.vo.request.BodyPartDictUpdateVO;

import java.util.List;

public interface BodyPartDictService {
    List<BodyPartDict> listBodyParts();
    String createBodyPart(BodyPartDictCreateVO vo);
    String updateBodyPart(BodyPartDictUpdateVO vo);
    String deleteBodyPart(int id);
}
