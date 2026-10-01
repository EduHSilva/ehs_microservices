package com.edu.silva.crm.controllers;

import com.edu.silva.common.DefaultResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {
    @GetMapping("/ping")
    public DefaultResponse<Void> index() {
        return new DefaultResponse<Void>("pong");
    }
}
