package cn.gugufish.service;

import cn.gugufish.entity.vo.response.DoctorDirectoryVO;
import cn.gugufish.entity.vo.response.UserPrescriptionVO;

import java.util.List;

public interface UserPortalService {
    List<DoctorDirectoryVO> listDoctors();
    List<UserPrescriptionVO> listPrescriptions(int accountId);
}
