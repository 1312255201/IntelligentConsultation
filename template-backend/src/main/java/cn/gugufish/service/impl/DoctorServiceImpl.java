package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.HomepageCase;
import cn.gugufish.entity.dto.HomepageRecommendDoctor;
import cn.gugufish.entity.vo.request.DoctorCreateVO;
import cn.gugufish.entity.vo.request.DoctorUpdateVO;
import cn.gugufish.mapper.DepartmentMapper;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.HomepageCaseMapper;
import cn.gugufish.mapper.HomepageRecommendDoctorMapper;
import cn.gugufish.service.DoctorService;
import cn.gugufish.service.ImageService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements DoctorService {

    @Resource
    DepartmentMapper departmentMapper;

    @Resource
    ImageService imageService;

    @Resource
    HomepageRecommendDoctorMapper homepageRecommendDoctorMapper;

    @Resource
    HomepageCaseMapper homepageCaseMapper;

    @Override
    public List<Doctor> listDoctors() {
        return this.list(Wrappers.<Doctor>query()
                .orderByAsc("sort")
                .orderByAsc("id"));
    }

    @Override
    public String createDoctor(DoctorCreateVO vo) {
        Department department = departmentMapper.selectById(vo.getDepartmentId());
        if (department == null) return "绑定的科室不存在";

        Date now = new Date();
        Doctor doctor = new Doctor(
                null,
                vo.getDepartmentId(),
                vo.getName(),
                vo.getTitle(),
                vo.getPhoto(),
                vo.getIntroduction(),
                vo.getExpertise(),
                vo.getSort(),
                vo.getStatus(),
                now,
                now
        );
        return this.save(doctor) ? null : "医生新增失败，请联系管理员";
    }

    @Override
    public String updateDoctor(DoctorUpdateVO vo) {
        Doctor doctor = this.getById(vo.getId());
        if (doctor == null) return "医生不存在";

        Department department = departmentMapper.selectById(vo.getDepartmentId());
        if (department == null) return "绑定的科室不存在";

        String oldPhoto = doctor.getPhoto();
        String nextPhoto = vo.getPhoto();

        boolean updated = this.update(Wrappers.<Doctor>update()
                .eq("id", vo.getId())
                .set("department_id", vo.getDepartmentId())
                .set("name", vo.getName())
                .set("title", vo.getTitle())
                .set("photo", nextPhoto)
                .set("introduction", vo.getIntroduction())
                .set("expertise", vo.getExpertise())
                .set("sort", vo.getSort())
                .set("status", vo.getStatus())
                .set("update_time", new Date()));
        if (!updated) {
            return "医生更新失败，请联系管理员";
        }

        if (oldPhoto != null && !oldPhoto.equals(nextPhoto)) {
            imageService.deleteImage(oldPhoto);
        }
        return null;
    }

    @Override
    public String deleteDoctor(int id) {
        Doctor doctor = this.getById(id);
        if (doctor == null) return "医生不存在";
        if (homepageRecommendDoctorMapper.exists(Wrappers.<HomepageRecommendDoctor>query().eq("doctor_id", id))) {
            return "当前医生已被首页推荐医生引用，请先在主页设置中解除关联后再删除";
        }
        if (homepageCaseMapper.exists(Wrappers.<HomepageCase>query().eq("doctor_id", id))) {
            return "当前医生已被首页经典案例引用，请先在主页设置中解除关联后再删除";
        }

        boolean removed = this.removeById(id);
        if (!removed) {
            return "医生删除失败，请联系管理员";
        }
        imageService.deleteImage(doctor.getPhoto());
        return null;
    }
}
