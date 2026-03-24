package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.dto.HomepageConfig;
import cn.gugufish.entity.vo.request.HomepageCaseCreateVO;
import cn.gugufish.entity.vo.request.HomepageCaseUpdateVO;
import cn.gugufish.entity.vo.request.HomepageConfigSaveVO;
import cn.gugufish.entity.vo.request.HomepageRecommendDoctorCreateVO;
import cn.gugufish.entity.vo.request.HomepageRecommendDoctorUpdateVO;
import cn.gugufish.entity.vo.response.HomepageCaseVO;
import cn.gugufish.entity.vo.response.HomepageConfigVO;
import cn.gugufish.entity.vo.response.HomepageRecommendDoctorVO;
import cn.gugufish.service.HomepageService;
import cn.gugufish.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.function.Supplier;

@Validated
@RestController
@RequestMapping("/api/admin/homepage")
@Tag(name = "管理员主页设置", description = "管理员对系统主页展示内容进行维护")
public class AdminHomepageController {

    @Resource
    HomepageService homepageService;

    @Resource
    ImageService imageService;

    @GetMapping("/config")
    @Operation(summary = "读取首页基础信息配置")
    public RestBean<HomepageConfigVO> config() {
        HomepageConfig config = homepageService.getConfig();
        return RestBean.success(config.asViewObject(HomepageConfigVO.class));
    }

    @PostMapping("/config")
    @Operation(summary = "保存首页基础信息配置")
    public RestBean<Void> saveConfig(@RequestBody @Valid HomepageConfigSaveVO vo) {
        return this.messageHandle(() -> homepageService.saveConfig(vo));
    }

    @GetMapping("/recommend-doctor/list")
    @Operation(summary = "查询首页推荐医生")
    public RestBean<List<HomepageRecommendDoctorVO>> listRecommendDoctors() {
        return RestBean.success(homepageService.listRecommendDoctors().stream()
                .map(item -> item.asViewObject(HomepageRecommendDoctorVO.class))
                .toList());
    }

    @PostMapping("/recommend-doctor/create")
    @Operation(summary = "新增首页推荐医生")
    public RestBean<Void> createRecommendDoctor(@RequestBody @Valid HomepageRecommendDoctorCreateVO vo) {
        return this.messageHandle(() -> homepageService.createRecommendDoctor(vo));
    }

    @PostMapping("/recommend-doctor/update")
    @Operation(summary = "更新首页推荐医生")
    public RestBean<Void> updateRecommendDoctor(@RequestBody @Valid HomepageRecommendDoctorUpdateVO vo) {
        return this.messageHandle(() -> homepageService.updateRecommendDoctor(vo));
    }

    @GetMapping("/recommend-doctor/delete")
    @Operation(summary = "删除首页推荐医生")
    public RestBean<Void> deleteRecommendDoctor(@RequestParam @Positive int id) {
        return this.messageHandle(() -> homepageService.deleteRecommendDoctor(id));
    }

    @GetMapping("/case/list")
    @Operation(summary = "查询经典案例")
    public RestBean<List<HomepageCaseVO>> listCases() {
        return RestBean.success(homepageService.listCases().stream()
                .map(item -> item.asViewObject(HomepageCaseVO.class))
                .toList());
    }

    @PostMapping("/case/create")
    @Operation(summary = "新增经典案例")
    public RestBean<Void> createCase(@RequestBody @Valid HomepageCaseCreateVO vo) {
        return this.messageHandle(() -> homepageService.createCase(vo));
    }

    @PostMapping("/case/update")
    @Operation(summary = "更新经典案例")
    public RestBean<Void> updateCase(@RequestBody @Valid HomepageCaseUpdateVO vo) {
        return this.messageHandle(() -> homepageService.updateCase(vo));
    }

    @GetMapping("/case/delete")
    @Operation(summary = "删除经典案例")
    public RestBean<Void> deleteCase(@RequestParam @Positive int id) {
        return this.messageHandle(() -> homepageService.deleteCase(id));
    }

    @PostMapping("/upload-image")
    @Operation(summary = "上传首页展示图片")
    public RestBean<String> uploadImage(@RequestParam("file") MultipartFile file,
                                        @RequestParam(defaultValue = "case")
                                        @Pattern(regexp = "(case|banner|section)") String type) throws Exception {
        if (file.getSize() > 1024 * 1024 * 3) {
            return RestBean.failure(400, "首页图片不能大于 3MB");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            return RestBean.failure(400, "首页图片必须是图片格式");
        }
        String url = imageService.uploadHomepageImage(type, file);
        return url != null ? RestBean.success(url) : RestBean.failure(400, "首页图片上传失败，请联系管理员");
    }

    private <T> RestBean<T> messageHandle(Supplier<String> action) {
        String message = action.get();
        if (message == null) {
            return RestBean.success();
        } else {
            return RestBean.failure(400, message);
        }
    }
}
