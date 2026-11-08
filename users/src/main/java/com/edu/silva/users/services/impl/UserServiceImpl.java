package com.edu.silva.users.services.impl;

import com.edu.silva.common.AuditLogger;
import com.edu.silva.common.enums.Status;
import com.edu.silva.users.domain.dtos.requests.AuthRequestDTO;
import com.edu.silva.users.domain.dtos.requests.RegisterRequestDTO;
import com.edu.silva.users.domain.dtos.requests.UpdateUserRequestDTO;
import com.edu.silva.users.domain.dtos.responses.LoginResponseDTO;
import com.edu.silva.users.domain.dtos.responses.UserResponseDTO;
import com.edu.silva.users.domain.dtos.responses.UserStatsResponseDTO;
import com.edu.silva.users.domain.entities.Company;
import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.domain.enums.UserRole;
import com.edu.silva.users.domain.enums.UserStatus;
import com.edu.silva.users.domain.producers.UserProducer;
import com.edu.silva.users.infra.exceptions.CustomExceptions;
import com.edu.silva.users.infra.security.TokenService;
import com.edu.silva.users.infra.security.annotattions.OnlyAdmin;
import com.edu.silva.users.repositories.CompanyRepository;
import com.edu.silva.users.repositories.UserRepository;
import com.edu.silva.users.services.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserServiceImpl implements UserService {

    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final List<String> SORT_FIELDS = List.of("username", "email", "status", "createdDate", "lastLogin");
    private final Map<UUID, Instant> resendCooldowns = new ConcurrentHashMap<>();

    @Value(value = "${google.client-id}")
    private String clientID;

    private final UserRepository repository;
    private final CompanyRepository companyRepository;
    private final UserProducer producer;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public UserServiceImpl(UserRepository userRepository, CompanyRepository companyRepository, UserProducer producer, AuthenticationManager authenticationManager, TokenService tokenService) {
        this.repository = userRepository;
        this.companyRepository = companyRepository;
        this.producer = producer;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @Transactional
    public UserResponseDTO save(RegisterRequestDTO dto) {
        if (repository.findByEmail(dto.email()) != null) {
            throw new CustomExceptions.EmailAlreadyExistsException(dto.email());
        }
        if (dto.role().equals(UserRole.ADMIN)) {
            throw new CustomExceptions.InvalidRoleException("User cannot register with admin role");
        }
        String encryptedPassword = new BCryptPasswordEncoder().encode(dto.password());
        User user = new User(dto.username(), dto.email(), encryptedPassword, Collections.singletonList(dto.role()));
        user.setStatus(UserStatus.WAITING_CONFIRMATION);

        Company company = null;
        if (dto.companyID() != null) {
            company = companyRepository.findById(dto.companyID())
                    .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Company.class.getSimpleName(), dto.companyID()));
        }

        // O usuário precisa existir antes da empresa (que guarda o owner) e a
        // empresa precisa existir antes do vínculo para não deixar company_id nulo.
        repository.save(user);

        if (company != null) {
            user.setCompany(company);
        } else {
            String companyName = null;
            switch (dto.role()) {
                // todo internacionalizar
                case HEALTH_NUTRITIONIST -> companyName = dto.username() + " nutricionista";
                case HEALTH_PERSONAL -> companyName = dto.username() + " personal";
                case BUDGETS -> companyName = dto.username();
            }

            if (companyName != null) {
                company = new Company();
                company.setOwner(user);
                company.setStatus(Status.ACTIVE);
                company.setName(companyName);
                companyRepository.save(company);
                user.setCompany(company);
            }
        }

        producer.publishMessageEmail(user);
        return new UserResponseDTO(user);
    }

    @Override
    public void delete(UUID id) {
        // O @SQLDelete do User dispara na remoção da entidade, mas a Company
        // (owner) ainda referencia o usuário e o flush quebra. O soft delete
        // explícito faz o mesmo UPDATE sem remover a instância do contexto.
        repository.findById(id).ifPresent(user -> {
            user.setStatus(UserStatus.DELETED);
            repository.save(user);
            AuditLogger.log(currentActor(), "USER_DELETE", User.class.getSimpleName(), id,
                    Map.of("email", user.getEmail()));
        });
    }

    @Override
    @OnlyAdmin
    public Page<@NonNull UserResponseDTO> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findAll(pageable).map(UserResponseDTO::new);
    }

    @Override
    @OnlyAdmin
    @Transactional
    public Page<@NonNull UserResponseDTO> search(String q, UserStatus status, UserRole role, int page, int size, String sort, String direction) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort, direction));
        return repository.findAll(specification(q, status, role), pageable).map(UserResponseDTO::new);
    }

    @Override
    @OnlyAdmin
    @Transactional
    public String exportCsv(String q, UserStatus status, UserRole role) {
        List<User> users = repository.findAll(specification(q, status, role), Sort.by(Sort.Direction.DESC, "createdDate"));

        StringBuilder csv = new StringBuilder(CsvUtils.BOM);
        csv.append(CsvUtils.line("id", "username", "email", "status", "roles", "company", "createdDate", "lastLogin"));
        for (User user : users) {
            csv.append(CsvUtils.line(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getStatus(),
                    CsvUtils.join(user.getRoles()),
                    user.getCompany() == null ? "" : user.getCompany().getName(),
                    CsvUtils.date(user.getCreatedDate()),
                    CsvUtils.date(user.getLastLogin())
            ));
        }
        return csv.toString();
    }

    @Override
    @OnlyAdmin
    @Transactional
    public UserStatsResponseDTO stats(LocalDate from, LocalDate to, String interval) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(29);

        if (start.isAfter(end)) {
            throw new CustomExceptions.InvalidStatusException("Período inválido: data inicial após a data final");
        }

        String unit = interval == null || interval.isBlank() ? "day" : interval.toLowerCase();
        if (!List.of("day", "week", "month").contains(unit)) {
            throw new CustomExceptions.InvalidStatusException("Intervalo inválido: use day, week ou month");
        }

        if ("day".equals(unit) && ChronoUnit.DAYS.between(start, end) > 366) {
            start = end.minusDays(366);
        }

        LocalDate filterStart = start;
        Map<LocalDate, long[]> buckets = new LinkedHashMap<>();
        for (LocalDate cursor = align(start, unit); !cursor.isAfter(end); cursor = next(cursor, unit)) {
            buckets.put(cursor, new long[2]);
        }

        repository.findAllCreatedDates().forEach(date -> increment(buckets, date, filterStart, end, unit, 0));
        companyRepository.findAllCreatedDates().forEach(date -> increment(buckets, date, filterStart, end, unit, 1));

        List<UserStatsResponseDTO.Point> series = buckets.entrySet().stream()
                .map(entry -> new UserStatsResponseDTO.Point(
                        label(entry.getKey(), unit),
                        entry.getValue()[0],
                        entry.getValue()[1]
                ))
                .toList();

        return new UserStatsResponseDTO(
                repository.count(),
                companyRepository.count(),
                repository.countByStatus(UserStatus.WAITING_CONFIRMATION),
                repository.countByStatus(UserStatus.BLOCKED),
                repository.countByCompanyIsNull(),
                series
        );
    }

    private LocalDate align(LocalDate date, String unit) {
        return switch (unit) {
            case "week" -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "month" -> date.withDayOfMonth(1);
            default -> date;
        };
    }

    private LocalDate next(LocalDate date, String unit) {
        return switch (unit) {
            case "week" -> date.plusWeeks(1);
            case "month" -> date.plusMonths(1);
            default -> date.plusDays(1);
        };
    }

    private void increment(Map<LocalDate, long[]> buckets, Date createdAt, LocalDate start, LocalDate end, String unit, int index) {
        if (createdAt == null) {
            return;
        }

        LocalDate date = createdAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (date.isBefore(start) || date.isAfter(end)) {
            return;
        }

        long[] values = buckets.get(align(date, unit));
        if (values != null) {
            values[index]++;
        }
    }

    private String label(LocalDate date, String unit) {
        return "month".equals(unit)
                ? date.format(DateTimeFormatter.ofPattern("MM/yyyy"))
                : date.format(DateTimeFormatter.ofPattern("dd/MM"));
    }

    private Sort buildSort(String sort, String direction) {        String property = SORT_FIELDS.contains(sort) ? sort : "createdDate";
        Sort.Direction order = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(order, property);
    }

    private Specification<User> specification(String q, UserStatus status, UserRole role) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(root.get("email")), like)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (role != null) {
                predicates.add(cb.equal(root.join("roles"), role));
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public UserResponseDTO findById(UUID id) {
        return new UserResponseDTO(repository.findById(id)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(User.class.getSimpleName(), id)));
    }

    @Override
    public UserResponseDTO update(UUID id, UpdateUserRequestDTO updateUserRequest) {
        return repository.findById(id)
                .map(user -> {
                    user.setUsername(updateUserRequest.username());

                    if (updateUserRequest.email() != null && !updateUserRequest.email().isBlank()) {
                        var existing = repository.findByEmail(updateUserRequest.email());
                        if (existing != null && !((User) existing).getId().equals(id)) {
                            throw new CustomExceptions.EmailAlreadyExistsException(updateUserRequest.email());
                        }
                        user.setEmail(updateUserRequest.email());
                    }

                    if (updateUserRequest.roles() != null && !updateUserRequest.roles().isEmpty()) {
                        if (updateUserRequest.roles().contains(UserRole.ADMIN) && !isAdmin()) {
                            throw new CustomExceptions.InvalidRoleException("Somente administradores podem atribuir a permissão ADMIN");
                        }
                        user.setRoles(new ArrayList<>(updateUserRequest.roles()));
                    }

                    User saved = repository.save(user);
                    AuditLogger.log(currentActor(), "USER_UPDATE", User.class.getSimpleName(), id,
                            Map.of("username", saved.getUsername(), "email", saved.getEmail()));
                    return new UserResponseDTO(saved);
                })
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(User.class.getSimpleName(), id));
    }

    @Override
    public UserResponseDTO updateStatus(UUID id, UserStatus status) {
        if (status != UserStatus.OK && status != UserStatus.BLOCKED) {
            throw new CustomExceptions.InvalidStatusException("Status deve ser OK ou BLOCKED");
        }
        return repository.findById(id)
                .map(user -> {
                    UserStatus previous = user.getStatus();
                    user.setStatus(status);
                    if (status == UserStatus.BLOCKED) {
                        user.setTokenVersion(user.getTokenVersion() + 1);
                    }
                    User saved = repository.save(user);
                    AuditLogger.log(currentActor(), "USER_STATUS", User.class.getSimpleName(), id,
                            Map.of("from", String.valueOf(previous), "to", String.valueOf(status)));
                    return new UserResponseDTO(saved);
                })
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(User.class.getSimpleName(), id));
    }

    @Override
    public void resendConfirmation(UUID id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(User.class.getSimpleName(), id));

        if (user.getStatus() != UserStatus.WAITING_CONFIRMATION) {
            throw new CustomExceptions.InvalidStatusException("Reenvio disponível apenas para usuários aguardando confirmação");
        }

        Instant lastSent = resendCooldowns.get(id);
        Instant now = Instant.now();
        if (lastSent != null && lastSent.plus(RESEND_COOLDOWN).isAfter(now)) {
            throw new CustomExceptions.InvalidStatusException("Aguarde 60 segundos antes de reenviar novamente");
        }
        resendCooldowns.put(id, now);

        producer.publishMessageEmail(user);
        AuditLogger.log(currentActor(), "USER_RESEND_CONFIRMATION", User.class.getSimpleName(), id,
                Map.of("email", user.getEmail()));
    }

    @Override
    public void invalidateSessions(UUID id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(User.class.getSimpleName(), id));
        user.setTokenVersion(user.getTokenVersion() + 1);
        repository.save(user);
        AuditLogger.log(currentActor(), "USER_LOGOUT_ALL", User.class.getSimpleName(), id, Map.of());
    }

    @Override
    public UserResponseDTO confirm(UUID id) {
        return repository.findById(id)
                .map(user -> {
                    if (user.getStatus() == UserStatus.WAITING_CONFIRMATION) {
                        user.setStatus(UserStatus.OK);
                        User saved = repository.save(user);
                        AuditLogger.log(currentActor(), "USER_CONFIRM", User.class.getSimpleName(), id,
                                Map.of("email", saved.getEmail()));
                        return new UserResponseDTO(saved);
                    } else {
                        throw new CustomExceptions.InvalidStatusException("Invalid status for user confirmation");
                    }
                })
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(User.class.getSimpleName(), id));
    }

    @Override
    public LoginResponseDTO login(AuthRequestDTO dto) {
        User user;
        if (dto.tokenGmail() != null && !dto.tokenGmail().isEmpty()) {
            var payload = verifyToken(dto.tokenGmail());

            if (payload == null) {
                throw new CustomExceptions.InvalidGmailTokenException("Invalid gmail token");
            }

            String email = payload.getEmail();
            user = (User) repository.findByEmail(email);
        } else {
            UsernamePasswordAuthenticationToken userNamePassword =
                    new UsernamePasswordAuthenticationToken(dto.email(), dto.password());

            Authentication authentication = authenticationManager.authenticate(userNamePassword);
            user = (User) authentication.getPrincipal();
        }

        if (user == null) {
            throw new CustomExceptions.EntityNotFoundException(User.class.getSimpleName(), dto.email());
        }

        String token = tokenService.generateToken(user);

        user.setLastLogin(new Date());
        repository.save(user);
        return new LoginResponseDTO(user.getId(), user.getUsername(), user.getEmail(), user.getLastLogin(), token);
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User principal)) {
            return "anonymous";
        }
        return String.valueOf(principal.getId());
    }

    private GoogleIdToken.Payload verifyToken(String token) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(clientID))
                    .build();

            GoogleIdToken idToken = verifier.verify(token);

            if (idToken != null) {
                return idToken.getPayload();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
