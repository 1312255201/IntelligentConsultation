package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.response.AdminConsultationRecordVO;
import cn.gugufish.service.ConsultationRecordAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/admin/consultation-record")
@Tag(name = "管理员导诊记录中心", description = "管理员查看用户问诊记录、问诊答案和规则命中留痕")
public class AdminConsultationRecordController {

    @Resource
    ConsultationRecordAdminService consultationRecordAdminService;

    @GetMapping("/list")
    @Operation(summary = "查询导诊记录列表")
    public RestBean<List<AdminConsultationRecordVO>> list() {
        return RestBean.success(consultationRecordAdminService.listRecords());
    }

    @GetMapping("/detail")
    @Operation(summary = "查询导诊记录详情")
    public RestBean<AdminConsultationRecordVO> detail(@RequestParam @Positive int id) {
        AdminConsultationRecordVO record = consultationRecordAdminService.recordDetail(id);
        return record == null ? RestBean.failure(404, "导诊记录不存在") : RestBean.success(record);
    }
}
