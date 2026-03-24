package cn.gugufish.service;

import cn.gugufish.entity.dto.DoctorSchedule;
import cn.gugufish.entity.vo.request.DoctorScheduleCreateVO;
import cn.gugufish.entity.vo.request.DoctorScheduleUpdateVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DoctorScheduleService extends IService<DoctorSchedule> {
    List<DoctorSchedule> listDoctorSchedules();
    String createDoctorSchedule(DoctorScheduleCreateVO vo);
    String updateDoctorSchedule(DoctorScheduleUpdateVO vo);
    String deleteDoctorSchedule(int id);
}
