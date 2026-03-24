package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.Department;
import cn.gugufish.entity.dto.Doctor;
import cn.gugufish.entity.dto.HomepageCase;
import cn.gugufish.entity.dto.HomepageConfig;
import cn.gugufish.entity.dto.HomepageRecommendDoctor;
import cn.gugufish.entity.vo.request.HomepageCaseCreateVO;
import cn.gugufish.entity.vo.request.HomepageCaseUpdateVO;
import cn.gugufish.entity.vo.request.HomepageConfigSaveVO;
import cn.gugufish.entity.vo.request.HomepageRecommendDoctorCreateVO;
import cn.gugufish.entity.vo.request.HomepageRecommendDoctorUpdateVO;
import cn.gugufish.entity.vo.response.HomepageCasePublicVO;
import cn.gugufish.entity.vo.response.HomepageConfigVO;
import cn.gugufish.entity.vo.response.HomepageDepartmentPublicVO;
import cn.gugufish.entity.vo.response.HomepageLandingVO;
import cn.gugufish.entity.vo.response.HomepageRecommendDoctorPublicVO;
import cn.gugufish.mapper.DepartmentMapper;
import cn.gugufish.mapper.DoctorMapper;
import cn.gugufish.mapper.HomepageCaseMapper;
import cn.gugufish.mapper.HomepageConfigMapper;
import cn.gugufish.mapper.HomepageRecommendDoctorMapper;
import cn.gugufish.service.HomepageService;
import cn.gugufish.service.ImageService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class HomepageServiceImpl implements HomepageService {

    private static final int CONFIG_ID = 1;

    @Resource
    HomepageConfigMapper configMapper;

    @Resource
    HomepageRecommendDoctorMapper recommendDoctorMapper;

    @Resource
    HomepageCaseMapper homepageCaseMapper;

    @Resource
    DoctorMapper doctorMapper;

    @Resource
    DepartmentMapper departmentMapper;

    @Resource
    ImageService imageService;

    @Override
    public HomepageLandingVO getLanding() {
        HomepageLandingVO landing = new HomepageLandingVO();
        landing.setConfig(getConfig().asViewObject(HomepageConfigVO.class));
        landing.setDepartments(listEnabledDepartments());
        landing.setRecommendDoctors(listEnabledRecommendDoctors());
        landing.setCases(listEnabledCases());
        return landing;
    }

    @Override
    public HomepageConfig getConfig() {
        HomepageConfig config = configMapper.selectById(CONFIG_ID);
        if (config != null) {
            return config;
        }

        HomepageConfig defaultConfig = new HomepageConfig(
                CONFIG_ID,
                "智能问诊系统",
                "提供专业、可靠、便捷的在线问诊与健康管理服务",
                "欢迎访问智能问诊系统，在线咨询、医生推荐与健康管理服务已就绪。",
                "平台简介",
                "系统支持科室展示、推荐医生、经典案例与后续智能问诊能力扩展，可作为首页内容配置的统一入口。",
                "",
                new Date()
        );
        configMapper.insert(defaultConfig);
        return defaultConfig;
    }

    @Override
    public String saveConfig(HomepageConfigSaveVO vo) {
        HomepageConfig config = new HomepageConfig(
                CONFIG_ID,
                vo.getHeroTitle(),
                vo.getHeroSubtitle(),
                vo.getNoticeText(),
                vo.getIntroTitle(),
                vo.getIntroContent(),
                vo.getServicePhone(),
                new Date()
        );
        return configMapper.selectById(CONFIG_ID) == null
                ? (configMapper.insert(config) > 0 ? null : "首页基础信息保存失败，请联系管理员")
                : (configMapper.updateById(config) > 0 ? null : "首页基础信息保存失败，请联系管理员");
    }

    @Override
    public List<HomepageRecommendDoctor> listRecommendDoctors() {
        return recommendDoctorMapper.selectList(Wrappers.<HomepageRecommendDoctor>query()
                .orderByAsc("sort")
                .orderByAsc("id"));
    }

    @Override
    public String createRecommendDoctor(HomepageRecommendDoctorCreateVO vo) {
        if (!existsDoctor(vo.getDoctorId())) return "推荐的医生不存在";
        if (existsRecommendDoctor(vo.getDoctorId(), null)) return "该医生已经加入推荐列表";

        HomepageRecommendDoctor entity = new HomepageRecommendDoctor(
                null,
                vo.getDoctorId(),
                vo.getDisplayTitle(),
                vo.getRecommendReason(),
                vo.getSort(),
                vo.getStatus(),
                new Date(),
                new Date()
        );
        return recommendDoctorMapper.insert(entity) > 0 ? null : "推荐医生新增失败，请联系管理员";
    }

    @Override
    public String updateRecommendDoctor(HomepageRecommendDoctorUpdateVO vo) {
        HomepageRecommendDoctor current = recommendDoctorMapper.selectById(vo.getId());
        if (current == null) return "推荐医生记录不存在";
        if (!existsDoctor(vo.getDoctorId())) return "推荐的医生不存在";
        if (existsRecommendDoctor(vo.getDoctorId(), vo.getId())) return "该医生已经加入推荐列表";

        HomepageRecommendDoctor entity = new HomepageRecommendDoctor(
                vo.getId(),
                vo.getDoctorId(),
                vo.getDisplayTitle(),
                vo.getRecommendReason(),
                vo.getSort(),
                vo.getStatus(),
                current.getCreateTime(),
                new Date()
        );
        return recommendDoctorMapper.updateById(entity) > 0 ? null : "推荐医生更新失败，请联系管理员";
    }

    @Override
    public String deleteRecommendDoctor(int id) {
        HomepageRecommendDoctor entity = recommendDoctorMapper.selectById(id);
        if (entity == null) return "推荐医生记录不存在";
        return recommendDoctorMapper.deleteById(id) > 0 ? null : "推荐医生删除失败，请联系管理员";
    }

    @Override
    public List<HomepageCase> listCases() {
        return homepageCaseMapper.selectList(Wrappers.<HomepageCase>query()
                .orderByAsc("sort")
                .orderByAsc("id"));
    }

    @Override
    public String createCase(HomepageCaseCreateVO vo) {
        String validate = validateCaseReference(vo.getDepartmentId(), vo.getDoctorId());
        if (validate != null) return validate;

        HomepageCase entity = new HomepageCase(
                null,
                vo.getDepartmentId(),
                vo.getDoctorId(),
                vo.getTitle(),
                vo.getCover(),
                vo.getSummary(),
                vo.getDetail(),
                vo.getTags(),
                vo.getSort(),
                vo.getStatus(),
                new Date(),
                new Date()
        );
        return homepageCaseMapper.insert(entity) > 0 ? null : "经典案例新增失败，请联系管理员";
    }

    @Override
    public String updateCase(HomepageCaseUpdateVO vo) {
        HomepageCase current = homepageCaseMapper.selectById(vo.getId());
        if (current == null) return "经典案例不存在";

        String validate = validateCaseReference(vo.getDepartmentId(), vo.getDoctorId());
        if (validate != null) return validate;

        String oldCover = current.getCover();
        HomepageCase entity = new HomepageCase(
                vo.getId(),
                vo.getDepartmentId(),
                vo.getDoctorId(),
                vo.getTitle(),
                vo.getCover(),
                vo.getSummary(),
                vo.getDetail(),
                vo.getTags(),
                vo.getSort(),
                vo.getStatus(),
                current.getCreateTime(),
                new Date()
        );
        boolean updated = homepageCaseMapper.updateById(entity) > 0;
        if (!updated) return "经典案例更新失败，请联系管理员";

        if (oldCover != null && !oldCover.equals(vo.getCover())) {
            imageService.deleteImage(oldCover);
        }
        return null;
    }

    @Override
    public String deleteCase(int id) {
        HomepageCase entity = homepageCaseMapper.selectById(id);
        if (entity == null) return "经典案例不存在";
        boolean removed = homepageCaseMapper.deleteById(id) > 0;
        if (!removed) return "经典案例删除失败，请联系管理员";
        imageService.deleteImage(entity.getCover());
        return null;
    }

    private List<HomepageDepartmentPublicVO> listEnabledDepartments() {
        return departmentMapper.selectList(Wrappers.<Department>query()
                        .eq("status", 1)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .map(item -> item.asViewObject(HomepageDepartmentPublicVO.class))
                .toList();
    }

    private List<HomepageRecommendDoctorPublicVO> listEnabledRecommendDoctors() {
        return recommendDoctorMapper.selectList(Wrappers.<HomepageRecommendDoctor>query()
                        .eq("status", 1)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .map(this::convertRecommendDoctor)
                .filter(Objects::nonNull)
                .toList();
    }

    private HomepageRecommendDoctorPublicVO convertRecommendDoctor(HomepageRecommendDoctor recommendDoctor) {
        Doctor doctor = doctorMapper.selectById(recommendDoctor.getDoctorId());
        if (doctor == null || doctor.getStatus() == null || doctor.getStatus() != 1) {
            return null;
        }

        Department department = departmentMapper.selectById(doctor.getDepartmentId());
        if (department == null || department.getStatus() == null || department.getStatus() != 1) {
            return null;
        }

        HomepageRecommendDoctorPublicVO vo = new HomepageRecommendDoctorPublicVO();
        vo.setId(recommendDoctor.getId());
        vo.setDoctorId(doctor.getId());
        vo.setDepartmentId(department.getId());
        vo.setDepartmentName(department.getName());
        vo.setName(doctor.getName());
        vo.setTitle(doctor.getTitle());
        vo.setPhoto(doctor.getPhoto());
        vo.setIntroduction(doctor.getIntroduction());
        vo.setExpertise(doctor.getExpertise());
        vo.setDisplayTitle(recommendDoctor.getDisplayTitle());
        vo.setRecommendReason(recommendDoctor.getRecommendReason());
        vo.setSort(recommendDoctor.getSort());
        return vo;
    }

    private List<HomepageCasePublicVO> listEnabledCases() {
        return homepageCaseMapper.selectList(Wrappers.<HomepageCase>query()
                        .eq("status", 1)
                        .orderByAsc("sort")
                        .orderByAsc("id"))
                .stream()
                .map(this::convertCase)
                .filter(Objects::nonNull)
                .toList();
    }

    private HomepageCasePublicVO convertCase(HomepageCase homepageCase) {
        Department department = departmentMapper.selectById(homepageCase.getDepartmentId());
        if (department == null || department.getStatus() == null || department.getStatus() != 1) {
            return null;
        }

        Doctor doctor = homepageCase.getDoctorId() == null ? null : doctorMapper.selectById(homepageCase.getDoctorId());

        HomepageCasePublicVO vo = new HomepageCasePublicVO();
        vo.setId(homepageCase.getId());
        vo.setDepartmentId(department.getId());
        vo.setDepartmentName(department.getName());
        vo.setDoctorId(doctor != null ? doctor.getId() : null);
        vo.setDoctorName(doctor != null ? doctor.getName() : null);
        vo.setDoctorTitle(doctor != null ? doctor.getTitle() : null);
        vo.setTitle(homepageCase.getTitle());
        vo.setCover(homepageCase.getCover());
        vo.setSummary(homepageCase.getSummary());
        vo.setDetail(homepageCase.getDetail());
        vo.setTags(homepageCase.getTags());
        vo.setSort(homepageCase.getSort());
        return vo;
    }

    private boolean existsDoctor(int doctorId) {
        return doctorMapper.selectById(doctorId) != null;
    }

    private boolean existsRecommendDoctor(int doctorId, Integer ignoreId) {
        return recommendDoctorMapper.exists(Wrappers.<HomepageRecommendDoctor>query()
                .eq("doctor_id", doctorId)
                .ne(ignoreId != null, "id", ignoreId));
    }

    private String validateCaseReference(Integer departmentId, Integer doctorId) {
        Department department = departmentMapper.selectById(departmentId);
        if (department == null) return "关联的科室不存在";
        if (doctorId != null) {
            Doctor doctor = doctorMapper.selectById(doctorId);
            if (doctor == null) return "关联的医生不存在";
            if (doctor.getDepartmentId() == null || !doctor.getDepartmentId().equals(departmentId)) {
                return "所选医生与科室不匹配";
            }
        }
        return null;
    }
}
