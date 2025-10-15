package com.agileo.AGILEO.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
public class MainController {

    @RequestMapping(value = {"/{path:[^\\.]*}", "/"})
    public String forward() {
        return "forward:/index.html";
    }
}