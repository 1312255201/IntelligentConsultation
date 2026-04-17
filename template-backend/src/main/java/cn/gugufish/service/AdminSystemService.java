package cn.gugufish.service;

import cn.gugufish.entity.vo.request.AdminAccountPasswordResetVO;
import cn.gugufish.entity.vo.request.AdminAccountRoleUpdateVO;
import cn.gugufish.entity.vo.response.AdminAccountManageVO;
import cn.gugufish.entity.vo.response.AdminOperationLogVO;
import cn.gugufish.entity.vo.response.AdminOrderManageVO;
import cn.gugufish.entity.vo.response.AdminUserManageVO;

import java.util.List;

public interface AdminSystemService {
    List<AdminAccountManageVO> listAccounts();
    List<AdminUserManageVO> listUsers();
    List<AdminOrderManageVO> listOrders();
    List<AdminOperationLogVO> listOperationLogs();
    String updateAccountRole(AdminAccountRoleUpdateVO vo);
    String resetAccountPassword(AdminAccountPasswordResetVO vo);
}
