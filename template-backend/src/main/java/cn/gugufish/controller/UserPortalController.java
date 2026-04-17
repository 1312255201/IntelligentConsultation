package cn.gugufish.controller;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.response.DoctorDirectoryVO;
import cn.gugufish.entity.vo.response.UserPrescriptionVO;
import cn.gugufish.service.UserPortalService;
import cn.gugufish.utils.Const;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@Tag(name = "User Portal Query", description = "用户查询医生目录与个人处方记录")
public class UserPortalController {

    @Resource
    UserPortalService userPortalService;

    @GetMapping("/doctor/list")
    @Operation(summary = "查询医生信息目录")
    public RestBean<List<DoctorDirectoryVO>> listDoctors() {
        return RestBean.success(userPortalService.listDoctors());
    }

    @GetMapping("/prescription/list")
    @Operation(summary = "查询当前用户个人处方列表")
    public RestBean<List<UserPrescriptionVO>> listPrescriptions(@RequestAttribute(Const.ATTR_USER_ID) int accountId) {
        return RestBean.success(userPortalService.listPrescriptions(accountId));
    }
}
