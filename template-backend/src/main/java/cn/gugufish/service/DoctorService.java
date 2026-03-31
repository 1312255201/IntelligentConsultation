package cn.gugufish.service;

import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.vo.request.DoctorCreateVO;
import cn.gugufish.entity.vo.request.DoctorUpdateVO;
import cn.gugufish.entity.vo.response.DoctorAccountOptionVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DoctorService extends IService<Doctor> {
    List<Doctor> listDoctors();
    List<DoctorAccountOptionVO> listDoctorAccountOptions();
    String createDoctor(DoctorCreateVO vo);
    String updateDoctor(DoctorUpdateVO vo);
    String deleteDoctor(int id);
}
