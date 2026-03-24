package cn.gugufish.service;

import cn.gugufish.entity.dto.DoctorServiceTag;
import cn.gugufish.entity.vo.request.DoctorServiceTagCreateVO;
import cn.gugufish.entity.vo.request.DoctorServiceTagUpdateVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DoctorServiceTagService extends IService<DoctorServiceTag> {
    List<DoctorServiceTag> listDoctorServiceTags();
    String createDoctorServiceTag(DoctorServiceTagCreateVO vo);
    String updateDoctorServiceTag(DoctorServiceTagUpdateVO vo);
    String deleteDoctorServiceTag(int id);
}
