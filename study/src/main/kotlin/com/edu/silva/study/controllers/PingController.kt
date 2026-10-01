package com.edu.silva.study.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PingController {
    @GetMapping("/")
    fun index() : String {
        return "ok"
    }
}