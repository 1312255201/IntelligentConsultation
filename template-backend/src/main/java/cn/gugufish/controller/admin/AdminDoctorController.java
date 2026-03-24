package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.DoctorCreateVO;
import cn.gugufish.entity.vo.request.DoctorUpdateVO;
import cn.gugufish.entity.vo.response.DoctorVO;
import cn.gugufish.service.DoctorService;
import cn.gugufish.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin/doctor")
@Tag(name = "管理员医生维护", description = "管理员对医生信息进行新增、修改、删除和查询")
public class AdminDoctorController {

    @Resource
    DoctorService doctorService;

    @Resource
    ImageService imageService;

    @GetMapping("/list")
    @Operation(summary = "查询医生列表")
    public RestBean<List<DoctorVO>> list() {
        return RestBean.success(doctorService.listDoctors()
                .stream()
                .map(item -> item.asViewObject(DoctorVO.class))
                .toList());
    }

    @PostMapping("/create")
    @Operation(summary = "新增医生")
    public RestBean<Void> create(@RequestBody @Valid DoctorCreateVO vo) {
        return this.messageHandle(() -> doctorService.createDoctor(vo));
    }

    @PostMapping("/update")
    @Operation(summary = "更新医生")
    public RestBean<Void> update(@RequestBody @Valid DoctorUpdateVO vo) {
        return this.messageHandle(() -> doctorService.updateDoctor(vo));
    }

    @GetMapping("/delete")
    @Operation(summary = "删除医生")
    public RestBean<Void> delete(@RequestParam @Positive int id) {
        return this.messageHandle(() -> doctorService.deleteDoctor(id));
    }

    @PostMapping("/upload-photo")
    @Operation(summary = "上传医生照片")
    public RestBean<String> uploadPhoto(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.getSize() > 1024 * 1024 * 2) {
            return RestBean.failure(400, "医生照片不能大于2MB");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            return RestBean.failure(400, "医生照片必须是图片格式");
        }
        String url = imageService.uploadDoctorPhoto(file);
        return url != null ? RestBean.success(url) : RestBean.failure(400, "医生照片上传失败，请联系管理员");
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
