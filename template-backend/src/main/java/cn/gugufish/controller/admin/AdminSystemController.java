package cn.gugufish.controller.admin;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.request.AdminAccountPasswordResetVO;
import cn.gugufish.entity.vo.request.AdminAccountRoleUpdateVO;
import cn.gugufish.entity.vo.response.AdminAccountManageVO;
import cn.gugufish.entity.vo.response.AdminOperationLogVO;
import cn.gugufish.entity.vo.response.AdminOrderManageVO;
import cn.gugufish.entity.vo.response.AdminUserManageVO;
import cn.gugufish.service.AdminSystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Supplier;

@Validated
@RestController
@RequestMapping("/api/admin/system")
@Tag(name = "Admin System Management", description = "管理员查看系统登录账号、用户概览、订单信息和操作日志")
public class AdminSystemController {

    @Resource
    AdminSystemService adminSystemService;

    @GetMapping("/account/list")
    @Operation(summary = "查询系统登录账号列表")
    public RestBean<List<AdminAccountManageVO>> listAccounts() {
        return RestBean.success(adminSystemService.listAccounts());
    }

    @PostMapping("/account/role")
    @Operation(summary = "更新系统账号角色")
    public RestBean<Void> updateAccountRole(@RequestBody @Valid AdminAccountRoleUpdateVO vo) {
        return this.messageHandle(() -> adminSystemService.updateAccountRole(vo));
    }

    @PostMapping("/account/password/reset")
    @Operation(summary = "重置系统账号密码")
    public RestBean<Void> resetAccountPassword(@RequestBody @Valid AdminAccountPasswordResetVO vo) {
        return this.messageHandle(() -> adminSystemService.resetAccountPassword(vo));
    }

    @GetMapping("/user/list")
    @Operation(summary = "查询用户信息列表")
    public RestBean<List<AdminUserManageVO>> listUsers() {
        return RestBean.success(adminSystemService.listUsers());
    }

    @GetMapping("/order/list")
    @Operation(summary = "查询订单信息列表")
    public RestBean<List<AdminOrderManageVO>> listOrders() {
        return RestBean.success(adminSystemService.listOrders());
    }

    @GetMapping("/operation-log/list")
    @Operation(summary = "查询操作日志")
    public RestBean<List<AdminOperationLogVO>> listOperationLogs() {
        return RestBean.success(adminSystemService.listOperationLogs());
    }

    private <T> RestBean<T> messageHandle(Supplier<String> action) {
        String message = action.get();
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }
}
