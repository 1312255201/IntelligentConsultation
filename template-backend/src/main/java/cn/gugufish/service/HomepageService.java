package cn.gugufish.service;

import cn.gugufish.entity.dto.HomepageCase;
import cn.gugufish.entity.dto.HomepageConfig;
import cn.gugufish.entity.dto.HomepageRecommendDoctor;
import cn.gugufish.entity.vo.response.HomepageLandingVO;
import cn.gugufish.entity.vo.request.HomepageCaseCreateVO;
import cn.gugufish.entity.vo.request.HomepageCaseUpdateVO;
import cn.gugufish.entity.vo.request.HomepageConfigSaveVO;
import cn.gugufish.entity.vo.request.HomepageRecommendDoctorCreateVO;
import cn.gugufish.entity.vo.request.HomepageRecommendDoctorUpdateVO;

import java.util.List;

public interface HomepageService {
    HomepageLandingVO getLanding();
    HomepageConfig getConfig();
    String saveConfig(HomepageConfigSaveVO vo);
    List<HomepageRecommendDoctor> listRecommendDoctors();
    String createRecommendDoctor(HomepageRecommendDoctorCreateVO vo);
    String updateRecommendDoctor(HomepageRecommendDoctorUpdateVO vo);
    String deleteRecommendDoctor(int id);
    List<HomepageCase> listCases();
    String createCase(HomepageCaseCreateVO vo);
    String updateCase(HomepageCaseUpdateVO vo);
    String deleteCase(int id);
}
