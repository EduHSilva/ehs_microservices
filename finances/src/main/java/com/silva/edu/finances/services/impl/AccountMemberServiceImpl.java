package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.AccountInviteActionRequest;
import com.silva.edu.finances.domain.dtos.request.InviteAccountMemberRequest;
import com.silva.edu.finances.domain.dtos.response.AccountMemberResponse;
import com.silva.edu.finances.domain.dtos.response.AccountMembersResponse;
import com.silva.edu.finances.domain.dtos.response.IncomingAccountInviteResponse;
import com.silva.edu.finances.domain.entities.FinanceAccount;
import com.silva.edu.finances.domain.entities.FinanceAccountMember;
import com.silva.edu.finances.domain.enums.AccountMemberStatus;
import com.silva.edu.finances.infra.config.UserContext;
import com.silva.edu.finances.infra.exceptions.CustomExceptions;
import com.silva.edu.finances.infra.integration.AccountInviteNotifier;
import com.silva.edu.finances.infra.integration.UsersServiceClient;
import com.silva.edu.finances.repositories.FinanceAccountMemberRepository;
import com.silva.edu.finances.repositories.FinanceAccountRepository;
import com.silva.edu.finances.services.AccountMemberService;
import com.silva.edu.finances.services.FinanceAccountService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AccountMemberServiceImpl implements AccountMemberService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<AccountMemberStatus> VISIBLE_STATUSES =
            List.of(AccountMemberStatus.PENDING, AccountMemberStatus.ACTIVE);

    private final FinanceAccountService accounts;
    private final FinanceAccountRepository accountRepository;
    private final FinanceAccountMemberRepository members;
    private final UsersServiceClient users;
    private final AccountInviteNotifier notifier;
    private final long ttlHours;

    public AccountMemberServiceImpl(FinanceAccountService accounts, FinanceAccountRepository accountRepository,
                                    FinanceAccountMemberRepository members, UsersServiceClient users,
                                    AccountInviteNotifier notifier,
                                    @Value("${finances.invite.ttl-hours:168}") long ttlHours) {
        this.accounts = accounts;
        this.accountRepository = accountRepository;
        this.members = members;
        this.users = users;
        this.notifier = notifier;
        this.ttlHours = ttlHours <= 0 ? 168 : ttlHours;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountMembersResponse list(String accountId) {
        FinanceAccount account = accounts.requireAccount(accountId);
        boolean owner = account.getOwnerId().equals(UserContext.getUser());
        Optional<UsersServiceClient.Profile> ownerProfile = users.findById(account.getOwnerId());
        List<AccountMemberResponse> cards = members
                .findAllByAccountIdAndStatusInOrderByCreatedAtAsc(accountId, VISIBLE_STATUSES)
                .stream().map(member -> card(member, owner)).toList();
        return new AccountMembersResponse(accountId, account.getName(), account.getOwnerId(),
                ownerProfile.map(UsersServiceClient.Profile::username).orElse(null),
                ownerProfile.map(UsersServiceClient.Profile::email).orElse(null), cards);
    }

    @Override
    public AccountMemberResponse invite(String accountId, InviteAccountMemberRequest request) {
        FinanceAccount account = accounts.requireAccount(accountId);
        requireOwner(account);
        String email = normalizeEmail(request.email());
        Optional<UsersServiceClient.Profile> profile = users.findByEmail(email);

        FinanceAccountMember member = members.findByAccountIdAndEmail(accountId, email)
                .orElseGet(() -> {
                    FinanceAccountMember created = new FinanceAccountMember();
                    created.setAccountId(accountId);
                    created.setEmail(email);
                    return created;
                });
        if (member.getStatus() == AccountMemberStatus.ACTIVE)
            throw new CustomExceptions.ConflictException("This e-mail already has access to the account");

        UUID invitedUserId = profile.map(value -> UUID.fromString(value.id())).orElse(member.getUserId());
        if (invitedUserId != null && invitedUserId.equals(account.getOwnerId()))
            throw new IllegalArgumentException("The account owner cannot be invited");
        if (invitedUserId != null) {
            members.findByAccountIdAndUserId(accountId, invitedUserId)
                    .filter(other -> !other.getId().equals(member.getId()))
                    .ifPresent(other -> {
                        throw new CustomExceptions.ConflictException("This user is already linked to the account");
                    });
        }

        member.setUserId(invitedUserId);
        profile.map(UsersServiceClient.Profile::username).filter(name -> !name.isBlank()).ifPresent(member::setName);
        member.setStatus(AccountMemberStatus.PENDING);
        member.setToken(newToken());
        member.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(ttlHours));
        member.setAcceptedAt(null);
        members.save(member);
        sendInvite(member, account);
        return card(member, true);
    }

    @Override
    public AccountMemberResponse resend(String accountId, UUID memberId) {
        FinanceAccount account = accounts.requireAccount(accountId);
        requireOwner(account);
        FinanceAccountMember member = findMember(accountId, memberId);
        if (member.getStatus() != AccountMemberStatus.PENDING && member.getStatus() != AccountMemberStatus.EXPIRED)
            throw new IllegalArgumentException("Only pending invitations can be resent");
        member.setStatus(AccountMemberStatus.PENDING);
        member.setToken(newToken());
        member.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(ttlHours));
        members.save(member);
        sendInvite(member, account);
        return card(member, true);
    }

    @Override
    public void revoke(String accountId, UUID memberId) {
        FinanceAccount account = accounts.requireAccount(accountId);
        requireOwner(account);
        FinanceAccountMember member = findMember(accountId, memberId);
        if (member.getStatus() != AccountMemberStatus.PENDING && member.getStatus() != AccountMemberStatus.ACTIVE)
            throw new IllegalArgumentException("This invitation can no longer be cancelled");
        boolean removedAccess = member.getStatus() == AccountMemberStatus.ACTIVE;
        member.setStatus(AccountMemberStatus.REVOKED);
        member.setToken(null);
        members.save(member);
        if (removedAccess)
            notifier.sendRemoval(member.getEmail(), member.getName(), member.getUserId(), ownerName(account),
                    account.getName());
    }

    @Override
    public void accept(AccountInviteActionRequest request) {
        acceptInvite(requirePendingInvite(request.token()));
    }

    @Override
    public void decline(AccountInviteActionRequest request) {
        declineInvite(requirePendingInvite(request.token()));
    }

    @Override
    public void acceptById(UUID inviteId) {
        FinanceAccountMember member = requirePendingInvite(inviteId);
        requireInvitee(member);
        acceptInvite(member);
    }

    @Override
    public void declineById(UUID inviteId) {
        FinanceAccountMember member = requirePendingInvite(inviteId);
        requireInvitee(member);
        declineInvite(member);
    }

    private void acceptInvite(FinanceAccountMember member) {
        FinanceAccount account = accountRepository.findById(member.getAccountId())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException("FinanceAccount", member.getAccountId()));
        UUID caller = UserContext.getUser();
        if (account.getOwnerId().equals(caller))
            throw new IllegalArgumentException("The account owner cannot accept their own invitation");
        if (members.existsByAccountIdAndUserIdAndStatus(account.getId(), caller, AccountMemberStatus.ACTIVE))
            throw new CustomExceptions.ConflictException("You already have access to this account");

        Optional<UsersServiceClient.Profile> profile = users.findById(caller);
        profile.map(UsersServiceClient.Profile::email)
                .filter(email -> !normalizeEmail(email).equals(member.getEmail()))
                .ifPresent(email -> {
                    throw new CustomExceptions.ForbiddenException("This invitation was sent to another e-mail");
                });
        if (member.getUserId() != null && !member.getUserId().equals(caller))
            throw new CustomExceptions.ForbiddenException("This invitation was sent to another user");

        member.setUserId(caller);
        profile.map(UsersServiceClient.Profile::username)
                .filter(name -> !name.isBlank())
                .ifPresent(member::setName);
        member.setStatus(AccountMemberStatus.ACTIVE);
        member.setAcceptedAt(LocalDateTime.now(ZoneOffset.UTC));
        member.setToken(null);
        members.save(member);
    }

    private void declineInvite(FinanceAccountMember member) {
        member.setStatus(AccountMemberStatus.DECLINED);
        member.setToken(null);
        members.save(member);
    }

    @Override
    public List<IncomingAccountInviteResponse> incoming() {
        members.expireDue(LocalDateTime.now(ZoneOffset.UTC));
        UUID caller = UserContext.getUser();
        Map<UUID, FinanceAccountMember> pending = new LinkedHashMap<>();
        members.findAllByUserIdAndStatus(caller, AccountMemberStatus.PENDING).forEach(member -> pending.put(member.getId(), member));
        users.findById(caller).map(UsersServiceClient.Profile::email).filter(email -> !email.isBlank())
                .ifPresent(email -> members.findAllByEmailAndStatus(normalizeEmail(email), AccountMemberStatus.PENDING)
                        .forEach(member -> pending.put(member.getId(), member)));

        List<IncomingAccountInviteResponse> result = new ArrayList<>();
        for (FinanceAccountMember member : pending.values()) {
            FinanceAccount account = accountRepository.findById(member.getAccountId()).orElse(null);
            if (account == null) continue;
            Optional<UsersServiceClient.Profile> ownerProfile = users.findById(account.getOwnerId());
            result.add(new IncomingAccountInviteResponse(member.getId(), account.getId(), account.getName(),
                    account.getOwnerId(), ownerProfile.map(UsersServiceClient.Profile::username).orElse(null),
                    ownerProfile.map(UsersServiceClient.Profile::email).orElse(null), member.getEmail(),
                    member.getExpiresAt()));
        }
        return result;
    }

    private FinanceAccountMember requirePendingInvite(String token) {
        members.expireDue(LocalDateTime.now(ZoneOffset.UTC));
        FinanceAccountMember member = members.findByToken(token == null ? "" : token.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found or expired"));
        if (member.getStatus() != AccountMemberStatus.PENDING)
            throw new IllegalArgumentException("Invitation is no longer valid");
        return member;
    }

    private FinanceAccountMember requirePendingInvite(UUID inviteId) {
        members.expireDue(LocalDateTime.now(ZoneOffset.UTC));
        FinanceAccountMember member = members.findById(inviteId)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException("FinanceAccountMember", inviteId));
        if (member.getStatus() != AccountMemberStatus.PENDING)
            throw new IllegalArgumentException("Invitation is no longer valid");
        return member;
    }

    /** Sem token, o convite só pode ser aceito pelo usuário/e-mail convidado. */
    private void requireInvitee(FinanceAccountMember member) {
        UUID caller = UserContext.getUser();
        if (member.getUserId() != null) {
            if (!member.getUserId().equals(caller))
                throw new CustomExceptions.ForbiddenException("This invitation was sent to another user");
            return;
        }
        String email = users.findById(caller).map(UsersServiceClient.Profile::email).orElse(null);
        if (email == null || !normalizeEmail(email).equals(member.getEmail()))
            throw new CustomExceptions.ForbiddenException("This invitation was sent to another e-mail");
    }

    private FinanceAccountMember findMember(String accountId, UUID memberId) {
        return members.findByIdAndAccountId(memberId, accountId)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException("FinanceAccountMember", memberId));
    }

    private void requireOwner(FinanceAccount account) {
        if (!account.getOwnerId().equals(UserContext.getUser()))
            throw new CustomExceptions.ForbiddenException("Only the account owner can manage members");
    }

    private void sendInvite(FinanceAccountMember member, FinanceAccount account) {
        notifier.sendInvite(member.getEmail(), member.getName(), member.getUserId(), ownerName(account),
                account.getName(), member.getToken());
    }

    private String ownerName(FinanceAccount account) {
        return users.findById(account.getOwnerId())
                .map(UsersServiceClient.Profile::username)
                .filter(name -> !name.isBlank())
                .orElse("Um usuário do Finanças");
    }

    private AccountMemberResponse card(FinanceAccountMember member, boolean includeToken) {
        String token = includeToken && member.getStatus() == AccountMemberStatus.PENDING ? member.getToken() : null;
        return new AccountMemberResponse(member.getId(), member.getAccountId(), member.getEmail(), member.getName(),
                member.getUserId(), member.getStatus().name(), member.getExpiresAt(), member.getAcceptedAt(),
                member.getCreatedAt(), token);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String newToken() {
        byte[] buffer = new byte[24];
        RANDOM.nextBytes(buffer);
        return HexFormat.of().formatHex(buffer);
    }
}
