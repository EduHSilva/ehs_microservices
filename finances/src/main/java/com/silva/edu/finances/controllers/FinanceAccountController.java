package com.silva.edu.finances.controllers;

import com.edu.silva.common.DefaultResponse;
import com.silva.edu.finances.domain.dtos.request.CreateFinanceAccountRequest;
import com.silva.edu.finances.domain.dtos.response.FinanceAccountResponse;
import com.silva.edu.finances.services.FinanceAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class FinanceAccountController {
    private final FinanceAccountService service;
    public FinanceAccountController(FinanceAccountService service) { this.service = service; }

    @GetMapping("/accounts")
    public ResponseEntity<DefaultResponse<List<FinanceAccountResponse>>> list() {
        return ResponseEntity.ok(new DefaultResponse<>("Authorized accounts found", service.listMine()));
    }

    @PostMapping("/accounts")
    public ResponseEntity<DefaultResponse<FinanceAccountResponse>> create(@RequestBody @Valid CreateFinanceAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new DefaultResponse<>("Account created successfully", service.create(request)));
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<DefaultResponse<FinanceAccountResponse>> get(@PathVariable String accountId) {
        return ResponseEntity.ok(new DefaultResponse<>("Authorized account found", service.get(accountId)));
    }

}
