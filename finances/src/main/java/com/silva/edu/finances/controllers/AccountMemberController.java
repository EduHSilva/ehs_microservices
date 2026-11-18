package com.silva.edu.finances.controllers;

import com.edu.silva.common.DefaultResponse;
import com.silva.edu.finances.domain.dtos.request.AccountInviteActionRequest;
import com.silva.edu.finances.domain.dtos.request.InviteAccountMemberRequest;
import com.silva.edu.finances.domain.dtos.response.AccountMemberResponse;
import com.silva.edu.finances.domain.dtos.response.AccountMembersResponse;
import com.silva.edu.finances.domain.dtos.response.IncomingAccountInviteResponse;
import com.silva.edu.finances.services.AccountMemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class AccountMemberController {
    private final AccountMemberService service;

    public AccountMemberController(AccountMemberService service) {
        this.service = service;
    }

    @GetMapping("/accounts/{accountId}/members")
    public ResponseEntity<DefaultResponse<AccountMembersResponse>> list(@PathVariable String accountId) {
        return ResponseEntity.ok(new DefaultResponse<>("Account members found", service.list(accountId)));
    }

    @PostMapping("/accounts/{accountId}/members")
    public ResponseEntity<DefaultResponse<AccountMemberResponse>> invite(
            @PathVariable String accountId, @RequestBody @Valid InviteAccountMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new DefaultResponse<>("Invitation sent successfully", service.invite(accountId, request)));
    }

    @PostMapping("/accounts/{accountId}/members/{memberId}/resend")
    public ResponseEntity<DefaultResponse<AccountMemberResponse>> resend(
            @PathVariable String accountId, @PathVariable UUID memberId) {
        return ResponseEntity.ok(new DefaultResponse<>("Invitation resent successfully", service.resend(accountId, memberId)));
    }

    @DeleteMapping("/accounts/{accountId}/members/{memberId}")
    public ResponseEntity<DefaultResponse<Void>> revoke(@PathVariable String accountId, @PathVariable UUID memberId) {
        service.revoke(accountId, memberId);
        return ResponseEntity.ok(new DefaultResponse<>("Access revoked successfully"));
    }

    @GetMapping("/account-invites")
    public ResponseEntity<DefaultResponse<List<IncomingAccountInviteResponse>>> incoming() {
        return ResponseEntity.ok(new DefaultResponse<>("Pending invitations found", service.incoming()));
    }

    @PostMapping("/account-invites/accept")
    public ResponseEntity<DefaultResponse<Void>> accept(@RequestBody @Valid AccountInviteActionRequest request) {
        service.accept(request);
        return ResponseEntity.ok(new DefaultResponse<>("Invitation accepted successfully"));
    }

    @PostMapping("/account-invites/decline")
    public ResponseEntity<DefaultResponse<Void>> decline(@RequestBody @Valid AccountInviteActionRequest request) {
        service.decline(request);
        return ResponseEntity.ok(new DefaultResponse<>("Invitation declined successfully"));
    }

    /** Aceite por id, usado pelo app quando o convite chega por e-mail sem o token do link. */
    @PostMapping("/account-invites/{inviteId}/accept")
    public ResponseEntity<DefaultResponse<Void>> acceptById(@PathVariable UUID inviteId) {
        service.acceptById(inviteId);
        return ResponseEntity.ok(new DefaultResponse<>("Invitation accepted successfully"));
    }

    @PostMapping("/account-invites/{inviteId}/decline")
    public ResponseEntity<DefaultResponse<Void>> declineById(@PathVariable UUID inviteId) {
        service.declineById(inviteId);
        return ResponseEntity.ok(new DefaultResponse<>("Invitation declined successfully"));
    }
}
