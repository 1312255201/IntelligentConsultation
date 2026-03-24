package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class HomepageLandingVO {
    HomepageConfigVO config;
    List<HomepageDepartmentPublicVO> departments;
    List<HomepageRecommendDoctorPublicVO> recommendDoctors;
    List<HomepageCasePublicVO> cases;
}
