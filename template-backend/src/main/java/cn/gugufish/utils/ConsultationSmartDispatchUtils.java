package cn.gugufish.utils;

import cn.gugufish.entity.vo.response.ConsultationRecommendDoctorVO;
import cn.gugufish.entity.vo.response.ConsultationSmartDispatchVO;
import com.alibaba.fastjson2.JSON;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

public final class ConsultationSmartDispatchUtils {

    private ConsultationSmartDispatchUtils() {
    }

    public static ConsultationSmartDispatchVO build(Integer claimedDoctorId,
                                                    String claimedDoctorName,
                                                    String assignmentStatus,
                                                    Integer resultDoctorId,
                                                    String resultDoctorName,
                                                    String doctorCandidatesJson,
                                                    String recommendationReason) {
        List<ConsultationRecommendDoctorVO> candidates = parseDoctorCandidates(doctorCandidatesJson);
        ConsultationRecommendDoctorVO suggestedDoctor = resolveSuggestedDoctor(
                resultDoctorId,
                resultDoctorName,
                candidates
        );
        int candidateCount = Math.max(candidates.size(), suggestedDoctor == null ? 0 : 1);
        boolean claimed = "claimed".equalsIgnoreCase(trimToNull(assignmentStatus));
        String source = resolveRecommendationSource(resultDoctorId, resultDoctorName, suggestedDoctor);

        ConsultationSmartDispatchVO vo = new ConsultationSmartDispatchVO();
        vo.setRecommendationSource(source);
        vo.setCandidateCount(candidateCount);
        vo.setClaimedDoctorId(claimed ? claimedDoctorId : null);
        vo.setClaimedDoctorName(claimed ? trimToNull(claimedDoctorName) : null);
        vo.setRecommendationReason(trimToNull(recommendationReason));
        if (suggestedDoctor != null) {
            vo.setSuggestedDoctorId(suggestedDoctor.getId());
            vo.setSuggestedDoctorName(trimToNull(suggestedDoctor.getName()));
            vo.setSuggestedDoctorTitle(trimToNull(suggestedDoctor.getTitle()));
            vo.setSuggestedDoctorPhoto(trimToNull(suggestedDoctor.getPhoto()));
            vo.setSuggestedDoctorExpertise(trimToNull(suggestedDoctor.getExpertise()));
            vo.setSuggestedDoctorNextScheduleText(trimToNull(suggestedDoctor.getNextScheduleText()));
            if (!StringUtils.hasText(vo.getRecommendationReason())) {
                vo.setRecommendationReason(trimToNull(suggestedDoctor.getRecommendationSummary()));
            }
        }

        fillStatus(vo, claimed, claimedDoctorId, trimToNull(claimedDoctorName));
        return vo;
    }

    private static void fillStatus(ConsultationSmartDispatchVO vo,
                                   boolean claimed,
                                   Integer claimedDoctorId,
                                   String claimedDoctorName) {
        String suggestedDoctorName = trimToNull(vo.getSuggestedDoctorName());
        Integer suggestedDoctorId = vo.getSuggestedDoctorId();

        if (claimed) {
            if (suggestedDoctorId != null && Objects.equals(suggestedDoctorId, claimedDoctorId)) {
                vo.setStatus("claimed_by_suggested");
                vo.setStatusText("推荐医生已接手");
                vo.setHint((suggestedDoctorName == null ? "当前推荐医生" : suggestedDoctorName) + " 已按系统优先推荐接手当前问诊，可继续补充病情变化和检查资料。");
                return;
            }
            if (suggestedDoctorId != null || suggestedDoctorName != null) {
                vo.setStatus("claimed_by_other");
                vo.setStatusText("已有医生接手");
                vo.setHint("系统原先优先推荐给 " + (suggestedDoctorName == null ? "相关医生" : suggestedDoctorName)
                        + "，目前由 " + (claimedDoctorName == null ? "医生" : claimedDoctorName) + " 接手处理。");
                return;
            }
            vo.setStatus("claimed_in_department");
            vo.setStatusText("科室医生已接手");
            vo.setHint("当前已由 " + (claimedDoctorName == null ? "科室医生" : claimedDoctorName) + " 接手处理，可继续通过消息同步病情变化。");
            return;
        }

        if (suggestedDoctorId != null || suggestedDoctorName != null) {
            vo.setStatus("waiting_accept");
            vo.setStatusText("等待医生接手");
            vo.setHint("系统已结合当前分诊结果、科室和排班优先推荐给 "
                    + (suggestedDoctorName == null ? "相关医生" : suggestedDoctorName)
                    + "，等待医生接手。");
            return;
        }

        vo.setStatus("department_queue");
        vo.setStatusText("科室分诊队列");
        vo.setHint("当前先进入科室分诊队列，系统会由科室医生继续接手。");
    }

    private static String resolveRecommendationSource(Integer resultDoctorId,
                                                      String resultDoctorName,
                                                      ConsultationRecommendDoctorVO suggestedDoctor) {
        if (resultDoctorId != null || StringUtils.hasText(resultDoctorName)) return "triage_result";
        if (suggestedDoctor != null) return "candidate_pool";
        return "department_queue";
    }

    private static ConsultationRecommendDoctorVO resolveSuggestedDoctor(Integer resultDoctorId,
                                                                        String resultDoctorName,
                                                                        List<ConsultationRecommendDoctorVO> candidates) {
        if (resultDoctorId != null) {
            ConsultationRecommendDoctorVO target = candidates.stream()
                    .filter(item -> Objects.equals(item.getId(), resultDoctorId))
                    .findFirst()
                    .orElse(null);
            if (target != null) return target;
        }

        String normalizedName = trimToNull(resultDoctorName);
        if (normalizedName != null) {
            ConsultationRecommendDoctorVO target = candidates.stream()
                    .filter(item -> normalizedName.equals(trimToNull(item.getName())))
                    .findFirst()
                    .orElse(null);
            if (target != null) return target;
        }

        if (resultDoctorId != null || normalizedName != null) {
            ConsultationRecommendDoctorVO fallback = new ConsultationRecommendDoctorVO();
            fallback.setId(resultDoctorId);
            fallback.setName(normalizedName);
            return fallback;
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static List<ConsultationRecommendDoctorVO> parseDoctorCandidates(String doctorCandidatesJson) {
        if (!StringUtils.hasText(doctorCandidatesJson)) return List.of();
        try {
            List<ConsultationRecommendDoctorVO> candidates = JSON.parseArray(doctorCandidatesJson, ConsultationRecommendDoctorVO.class);
            return candidates == null ? List.of() : candidates.stream()
                    .filter(item -> item != null && (item.getId() != null || StringUtils.hasText(item.getName())))
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
