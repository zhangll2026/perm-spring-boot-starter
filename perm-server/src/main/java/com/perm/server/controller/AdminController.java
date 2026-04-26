package com.perm.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {
    @GetMapping("/perm-admin")
    public String index() { return "redirect:/perm-admin/index.html"; }
}
