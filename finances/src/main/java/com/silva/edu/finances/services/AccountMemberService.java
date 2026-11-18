package com.silva.edu.finances.services;

import com.silva.edu.finances.domain.dtos.request.AccountInviteActionRequest;
import com.silva.edu.finances.domain.dtos.request.InviteAccountMemberRequest;
import com.silva.edu.finances.domain.dtos.response.AccountMemberResponse;
import com.silva.edu.finances.domain.dtos.response.AccountMembersResponse;
import com.silva.edu.finances.domain.dtos.response.IncomingAccountInviteResponse;

import java.util.List;
import java.util.UUID;

public interface AccountMemberService {
    AccountMembersResponse list(String accountId);
    AccountMemberResponse invite(String accountId, InviteAccountMemberRequest request);
    AccountMemberResponse resend(String accountId, UUID memberId);
    void revoke(String accountId, UUID memberId);
    void accept(AccountInviteActionRequest request);
    void decline(AccountInviteActionRequest request);
    void acceptById(UUID inviteId);
    void declineById(UUID inviteId);
    List<IncomingAccountInviteResponse> incoming();
}
