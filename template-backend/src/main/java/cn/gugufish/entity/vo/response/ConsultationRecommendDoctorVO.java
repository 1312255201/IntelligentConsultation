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
    List<String> serviceTags;
}
