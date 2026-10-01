package com.edu.silva.study.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DashboardController {
    @GetMapping("/dashboard")
    fun index(): ResponseEntity<String> {
        return ResponseEntity(
            """
        {
          "weeklyProgress": {
            "completed": 14,
            "total": 20
          },
          "focus": {
            "remainingSeconds": 1125,
            "subject": "Escrita do Capítulo 1",
            "running": true
          },
          "reminders": [],
          "notes": [
            {
              "id": "1",
              "week": "Semana 3",
              "title": "Finalizar introdução",
              "subject": "Escrita"
            }
          ]
        }
        """.trimIndent(),
            HttpStatus.OK
        )
    }
}