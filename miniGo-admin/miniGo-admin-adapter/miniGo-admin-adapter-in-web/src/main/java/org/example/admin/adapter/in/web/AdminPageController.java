package org.example.admin.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping({"/admin", "/admin/"})
    public String home() {
        return "forward:/admin/login.html";
    }
}
