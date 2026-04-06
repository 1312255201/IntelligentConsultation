package cn.gugufish.entity.vo.response;

import lombok.Data;

import java.util.List;

@Data
public class ConsultationRecommendDoctorVO {
    Integer id;
    String name;
    String title;
    String photo;
    String expertise;
    String introduction;
    String nextScheduleText;
    Integer remainingCapacity;
    Integer activeConsultationCount;
    Integer recommendationScore;
    String recommendationSummary;
    List<String> recommendationReasons;
    List<String> serviceTags;
    List<String> matchedServiceTags;
}
