package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.DoctorReplyTemplate;
import cn.gugufish.entity.vo.request.DoctorReplyTemplateCreateVO;
import cn.gugufish.entity.vo.request.DoctorReplyTemplateUpdateVO;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.DoctorReplyTemplateMapper;
import cn.gugufish.service.DoctorReplyTemplateService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DoctorReplyTemplateServiceImpl extends ServiceImpl<DoctorReplyTemplateMapper, DoctorReplyTemplate> implements DoctorReplyTemplateService {

    @Resource
    DoctorMapper doctorMapper;

    @Override
    public List<DoctorReplyTemplate> listDoctorReplyTemplates(int accountId) {
        Doctor doctor = currentDoctor(accountId);
        if (doctor == null) return List.of();
        return this.list(Wrappers.<DoctorReplyTemplate>query()
                .eq("doctor_id", doctor.getId())
                .orderByAsc("sort")
                .orderByAsc("id"));
    }

    @Override
    public String createDoctorReplyTemplate(int accountId, DoctorReplyTemplateCreateVO vo) {
        Doctor doctor = currentDoctor(accountId);
        if (doctor == null) return "当前 doctor 账号尚未绑定有效医生档案";
        if (existsSameTitle(doctor.getId(), vo.getSceneType(), vo.getTitle(), null)) return "当前场景下已存在相同标题的模板";

        Date now = new Date();
        DoctorReplyTemplate template = new DoctorReplyTemplate(
                null,
                doctor.getId(),
                vo.getSceneType().trim(),
                vo.getTitle().trim(),
                vo.getContent().trim(),
                vo.getSort(),
                vo.getStatus(),
                now,
                now
        );
        return this.save(template) ? null : "常用回复模板新增失败，请稍后重试";
    }

    @Override
    public String updateDoctorReplyTemplate(int accountId, DoctorReplyTemplateUpdateVO vo) {
        Doctor doctor = currentDoctor(accountId);
        if (doctor == null) return "当前 doctor 账号尚未绑定有效医生档案";

        DoctorReplyTemplate current = this.getById(vo.getId());
        if (current == null || !doctor.getId().equals(current.getDoctorId())) return "常用回复模板不存在或暂无权限";
        if (existsSameTitle(doctor.getId(), vo.getSceneType(), vo.getTitle(), vo.getId())) return "当前场景下已存在相同标题的模板";

        boolean updated = this.update(Wrappers.<DoctorReplyTemplate>update()
                .eq("id", vo.getId())
                .eq("doctor_id", doctor.getId())
                .set("scene_type", vo.getSceneType().trim())
                .set("title", vo.getTitle().trim())
                .set("content", vo.getContent().trim())
                .set("sort", vo.getSort())
                .set("status", vo.getStatus())
                .set("update_time", new Date()));
        return updated ? null : "常用回复模板更新失败，请稍后重试";
    }

    @Override
    public String deleteDoctorReplyTemplate(int accountId, int id) {
        Doctor doctor = currentDoctor(accountId);
        if (doctor == null) return "当前 doctor 账号尚未绑定有效医生档案";

        DoctorReplyTemplate current = this.getById(id);
        if (current == null || !doctor.getId().equals(current.getDoctorId())) return "常用回复模板不存在或暂无权限";
        return this.remove(Wrappers.<DoctorReplyTemplate>query()
                .eq("id", id)
                .eq("doctor_id", doctor.getId())) ? null : "常用回复模板删除失败，请稍后重试";
    }

    private Doctor currentDoctor(int accountId) {
        return doctorMapper.selectOne(Wrappers.<Doctor>query()
                .eq("account_id", accountId)
                .last("limit 1"));
    }

    private boolean existsSameTitle(int doctorId, String sceneType, String title, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<DoctorReplyTemplate>query()
                .eq("doctor_id", doctorId)
                .eq("scene_type", sceneType.trim())
                .eq("title", title.trim())
                .ne(ignoreId != null, "id", ignoreId));
    }
}
