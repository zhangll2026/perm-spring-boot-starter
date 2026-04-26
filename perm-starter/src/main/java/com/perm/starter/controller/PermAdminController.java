package com.perm.starter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 管理界面入口控制器 - 重定向到Vue前端页面
 */
@Controller
public class PermAdminController {

    @GetMapping("/perm-admin")
    public String index() {
        return "redirect:/perm-admin/index.html";
    }
}
