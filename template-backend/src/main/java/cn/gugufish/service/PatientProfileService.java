package cn.gugufish.service;

import cn.gugufish.entity.dto.PatientProfile;
import cn.gugufish.entity.vo.request.PatientProfileCreateVO;
import cn.gugufish.entity.vo.request.PatientProfileUpdateVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PatientProfileService extends IService<PatientProfile> {
    List<PatientProfile> listByAccountId(int accountId);
    String createProfile(int accountId, PatientProfileCreateVO vo);
    String updateProfile(int accountId, PatientProfileUpdateVO vo);
    String deleteProfile(int accountId, int id);
}
