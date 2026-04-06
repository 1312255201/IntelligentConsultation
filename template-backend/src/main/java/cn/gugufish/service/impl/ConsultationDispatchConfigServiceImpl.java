package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.ConsultationDispatchConfig;
import cn.gugufish.entity.vo.request.ConsultationDispatchConfigSaveVO;
import cn.gugufish.mapper.ConsultationDispatchConfigMapper;
import cn.gugufish.service.ConsultationDispatchConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ConsultationDispatchConfigServiceImpl implements ConsultationDispatchConfigService {

    private static final int CONFIG_ID = 1;

    @Resource
    ConsultationDispatchConfigMapper consultationDispatchConfigMapper;

    @Override
    public ConsultationDispatchConfig getConfig() {
        ConsultationDispatchConfig config = consultationDispatchConfigMapper.selectById(CONFIG_ID);
        if (config != null) {
            return config;
        }

        ConsultationDispatchConfig defaultConfig = defaultConfig(new Date());
        consultationDispatchConfigMapper.insert(defaultConfig);
        return defaultConfig;
    }

    @Override
    public String saveConfig(ConsultationDispatchConfigSaveVO vo) {
        if (vo.getMinRecommendationScore() != null
                && vo.getMaxRecommendationScore() != null
                && vo.getMinRecommendationScore() > vo.getMaxRecommendationScore()) {
            return "推荐分下限不能大于上限";
        }

        ConsultationDispatchConfig config = new ConsultationDispatchConfig(
                CONFIG_ID,
                vo.getVisitTypeWeight(),
                vo.getScheduleWeight(),
                vo.getCapacityWeight(),
                vo.getWorkloadWeight(),
                vo.getTagMatchWeight(),
                vo.getTagMatchScorePerHit(),
                vo.getMaxMatchedTags(),
                vo.getRecommendDoctorLimit(),
                vo.getMinRecommendationScore(),
                vo.getMaxRecommendationScore(),
                vo.getWaitingOverdueHours(),
                new Date()
        );
        return consultationDispatchConfigMapper.selectById(CONFIG_ID) == null
                ? (consultationDispatchConfigMapper.insert(config) > 0 ? null : "智能分配策略保存失败，请稍后重试")
                : (consultationDispatchConfigMapper.updateById(config) > 0 ? null : "智能分配策略保存失败，请稍后重试");
    }

    private ConsultationDispatchConfig defaultConfig(Date updateTime) {
        return new ConsultationDispatchConfig(
                CONFIG_ID,
                100,
                100,
                100,
                100,
                100,
                4,
                3,
                3,
                24,
                99,
                24,
                updateTime
        );
    }
}
