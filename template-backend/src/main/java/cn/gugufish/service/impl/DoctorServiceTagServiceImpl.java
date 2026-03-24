package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.DoctorServiceTag;
import cn.gugufish.entity.vo.request.DoctorServiceTagCreateVO;
import cn.gugufish.entity.vo.request.DoctorServiceTagUpdateVO;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.DoctorServiceTagMapper;
import cn.gugufish.service.DoctorServiceTagService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DoctorServiceTagServiceImpl extends ServiceImpl<DoctorServiceTagMapper, DoctorServiceTag> implements DoctorServiceTagService {

    @Resource
    DoctorMapper doctorMapper;

    @Override
    public List<DoctorServiceTag> listDoctorServiceTags() {
        return this.list(Wrappers.<DoctorServiceTag>query()
                .orderByAsc("sort")
                .orderByAsc("id"));
    }

    @Override
    public String createDoctorServiceTag(DoctorServiceTagCreateVO vo) {
        if (!existsDoctor(vo.getDoctorId())) return "关联的医生不存在";
        if (existsSameTag(vo.getDoctorId(), vo.getTagCode(), null)) return "该医生已存在相同的服务标签编码";

        Date now = new Date();
        DoctorServiceTag tag = new DoctorServiceTag(
                null,
                vo.getDoctorId(),
                vo.getTagCode().trim(),
                vo.getTagName().trim(),
                vo.getSort(),
                vo.getStatus(),
                now,
                now
        );
        return this.save(tag) ? null : "医生服务标签新增失败，请联系管理员";
    }

    @Override
    public String updateDoctorServiceTag(DoctorServiceTagUpdateVO vo) {
        DoctorServiceTag current = this.getById(vo.getId());
        if (current == null) return "医生服务标签不存在";
        if (!existsDoctor(vo.getDoctorId())) return "关联的医生不存在";
        if (existsSameTag(vo.getDoctorId(), vo.getTagCode(), vo.getId())) return "该医生已存在相同的服务标签编码";

        boolean updated = this.update(Wrappers.<DoctorServiceTag>update()
                .eq("id", vo.getId())
                .set("doctor_id", vo.getDoctorId())
                .set("tag_code", vo.getTagCode().trim())
                .set("tag_name", vo.getTagName().trim())
                .set("sort", vo.getSort())
                .set("status", vo.getStatus())
                .set("update_time", new Date()));
        return updated ? null : "医生服务标签更新失败，请联系管理员";
    }

    @Override
    public String deleteDoctorServiceTag(int id) {
        DoctorServiceTag current = this.getById(id);
        if (current == null) return "医生服务标签不存在";
        return this.removeById(id) ? null : "医生服务标签删除失败，请联系管理员";
    }

    private boolean existsDoctor(int doctorId) {
        Doctor doctor = doctorMapper.selectById(doctorId);
        return doctor != null;
    }

    private boolean existsSameTag(int doctorId, String tagCode, Integer ignoreId) {
        return this.baseMapper.exists(Wrappers.<DoctorServiceTag>query()
                .eq("doctor_id", doctorId)
                .eq("tag_code", tagCode.trim())
                .ne(ignoreId != null, "id", ignoreId));
    }
}
