package com.edu.silva.users.services.impl;

import com.edu.silva.common.enums.Status;
import com.edu.silva.users.domain.dtos.requests.AuthRequestDTO;
import com.edu.silva.users.domain.dtos.requests.RegisterRequestDTO;
import com.edu.silva.users.domain.dtos.requests.UpdateUserRequestDTO;
import com.edu.silva.users.domain.dtos.responses.LoginResponseDTO;
import com.edu.silva.users.domain.dtos.responses.UserResponseDTO;
import com.edu.silva.users.domain.entities.Company;
import com.edu.silva.users.domain.entities.User;
import com.edu.silva.users.domain.enums.UserRole;
import com.edu.silva.users.domain.enums.UserStatus;
import com.edu.silva.users.domain.producers.UserProducer;
import com.edu.silva.users.infra.exceptions.CustomExceptions;
import com.edu.silva.users.infra.security.TokenService;
import com.edu.silva.users.infra.security.annotattions.OnlyOwner;
import com.edu.silva.users.repositories.CompanyRepository;
import com.edu.silva.users.repositories.UserRepository;
import com.edu.silva.users.services.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

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
        Company company = new Company();
        if (dto.companyID() != null) {
            Optional<Company> optionalCompany = companyRepository.findById(dto.companyID());
            if (optionalCompany.isPresent()) {
                company = optionalCompany.get();
            } else {
                throw new CustomExceptions.EntityNotFoundException(Company.class.getSimpleName(), dto.companyID());
            }
        } else {
            String companyName = null;
            switch (dto.role()) {
                // todo internacionalizar
                case HEALTH_NUTRITIONIST -> companyName = dto.username() + " nutricionista";
                case HEALTH_PERSONAL -> companyName = dto.username() + " personal";
                case CRM -> companyName = dto.username();
            }

            if (companyName != null) {
                company.setOwner(user);
                company.setStatus(Status.ACTIVE);
                company.setName(companyName);
            }
        }

        user.setStatus(UserStatus.WAITING_CONFIRMATION);
        user.setCompany(company);
        repository.save(user);
        companyRepository.save(company);
        producer.publishMessageEmail(user);
        return new UserResponseDTO(user);
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    @Override
    @OnlyOwner
    public Page<@NonNull UserResponseDTO> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findAll(pageable).map(UserResponseDTO::new);
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
                    return new UserResponseDTO(repository.save(user));
                })
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(User.class.getSimpleName(), id));
    }

    @Override
    public UserResponseDTO confirm(UUID id) {
        return repository.findById(id)
                .map(user -> {
                    if (user.getStatus() == UserStatus.WAITING_CONFIRMATION) {
                        user.setStatus(UserStatus.OK);
                        return new UserResponseDTO(repository.save(user));
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
