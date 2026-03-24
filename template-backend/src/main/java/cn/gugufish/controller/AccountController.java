package cn.gugufish.controller;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.dto.Account;
import cn.gugufish.entity.vo.request.ChangeEmailVO;
import cn.gugufish.entity.vo.request.ChangePasswordVO;
import cn.gugufish.entity.vo.response.AccountInfoVO;
import cn.gugufish.service.AccountService;
import cn.gugufish.utils.Const;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@Validated
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户资料管理", description = "当前登录用户的基础资料查询与修改")
public class AccountController {

    @Resource
    AccountService accountService;

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户资料")
    public RestBean<AccountInfoVO> me(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        Account account = accountService.findAccountById(id);
        if (account == null) {
            return RestBean.failure(404, "当前用户不存在");
        }
        return RestBean.success(account.asViewObject(AccountInfoVO.class));
    }

    @PostMapping("/change-email")
    @Operation(summary = "修改当前登录用户邮箱")
    public RestBean<Void> changeEmail(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                      @RequestBody @Valid ChangeEmailVO vo) {
        return this.messageHandle(() -> accountService.updateAccountEmail(id, vo));
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改当前登录用户密码")
    public RestBean<Void> changePassword(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                         @RequestBody @Valid ChangePasswordVO vo) {
        return this.messageHandle(() -> accountService.updateAccountPassword(id, vo));
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
