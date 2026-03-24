package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.DoctorSchedule;
import cn.gugufish.entity.vo.request.DoctorScheduleCreateVO;
import cn.gugufish.entity.vo.request.DoctorScheduleUpdateVO;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.DoctorScheduleMapper;
import cn.gugufish.service.DoctorScheduleService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DoctorScheduleServiceImpl extends ServiceImpl<DoctorScheduleMapper, DoctorSchedule> implements DoctorScheduleService {

    @Resource
    DoctorMapper doctorMapper;

    @Override
    public List<DoctorSchedule> listDoctorSchedules() {
        return this.list(Wrappers.<DoctorSchedule>query()
                .orderByAsc("schedule_date")
                .orderByAsc("time_period")
                .orderByAsc("id"));
    }

    @Override
    public String createDoctorSchedule(DoctorScheduleCreateVO vo) {
        if (!existsDoctor(vo.getDoctorId())) return "关联的医生不存在";
        if (vo.getUsedCapacity() > vo.getMaxCapacity()) return "已接诊量不能大于最大接诊量";
        if (existsSameSchedule(vo.getDoctorId(), vo.getScheduleDate(), vo.getTimePeriod(), vo.getVisitType(), null)) {
            return "该医生在相同日期、时段和接诊方式下已存在排班";
        }

        Date now = new Date();
        DoctorSchedule schedule = new DoctorSchedule(
                null,
                vo.getDoctorId(),
                vo.getScheduleDate(),
                vo.getTimePeriod(),
                vo.getVisitType(),
                vo.getMaxCapacity(),
                vo.getUsedCapacity(),
                vo.getStatus(),
                blankToNull(vo.getRemark()),
                now,
                now
        );
        return this.save(schedule) ? null : "医生排班新增失败，请联系管理员";
    }

    @Override
    public String updateDoctorSchedule(DoctorScheduleUpdateVO vo) {
        DoctorSchedule current = this.getById(vo.getId());
        if (current == null) return "医生排班不存在";
        if (!existsDoctor(vo.getDoctorId())) return "关联的医生不存在";
        if (vo.getUsedCapacity() > vo.getMaxCapacity()) return "已接诊量不能大于最大接诊量";
        if (existsSameSchedule(vo.getDoctorId(), vo.getScheduleDate(), vo.getTimePeriod(), vo.getVisitType(), vo.getId())) {
            return "该医生在相同日期、时段和接诊方式下已存在排班";
        }

        boolean updated = this.update(Wrappers.<DoctorSchedule>update()
                .eq("id", vo.getId())
                .set("doctor_id", vo.getDoctorId())
                .set("schedule_date", vo.getScheduleDate())
                .set("time_period", vo.getTimePeriod())
                .set("visit_type", vo.getVisitType())
                .set("max_capacity", vo.getMaxCapacity())
                .set("used_capacity", vo.getUsedCapacity())
                .set("status", vo.getStatus())
                .set("remark", blankToNull(vo.getRemark()))
                .set("update_time", new Date()));
        return updated ? null : "医生排班更新失败，请联系管理员";
    }

    @Override
    public String deleteDoctorSchedule(int id) {
        DoctorSchedule current = this.getById(id);
        if (current == null) return "医生排班不存在";
        return this.removeById(id) ? null : "医生排班删除失败，请联系管理员";
    }

    private boolean existsDoctor(int doctorId) {
        Doctor doctor = doctorMapper.selectById(doctorId);
        return doctor != null;
    }

    private boolean existsSameSchedule(int doctorId, Date scheduleDate, String timePeriod, String visitType, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<DoctorSchedule>query()
                .eq("doctor_id", doctorId)
                .eq("schedule_date", scheduleDate)
                .eq("time_period", timePeriod)
                .eq("visit_type", visitType)
                .ne(ignoreId != null, "id", ignoreId));
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
