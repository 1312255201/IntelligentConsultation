package cn.gugufish.controller;

import cn.gugufish.entity.RestBean;
import cn.gugufish.entity.vo.response.HomepageLandingVO;
import cn.gugufish.service.HomepageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/homepage")
@Tag(name = "网站首页", description = "公开访问的网站首页展示数据接口")
public class HomepageController {

    @Resource
    HomepageService homepageService;

    @GetMapping("/landing")
    @Operation(summary = "读取网站首页展示数据")
    public RestBean<HomepageLandingVO> landing() {
        return RestBean.success(homepageService.getLanding());
    }
}
