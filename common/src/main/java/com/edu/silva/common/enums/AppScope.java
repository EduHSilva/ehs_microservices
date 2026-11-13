package com.edu.silva.common.enums;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Aplicações (front-ends) para as quais um usuário pode se autenticar.
 *
 * <p>Cada aplicação declara as roles que dão acesso a ela. A role {@code ADMIN}
 * tem acesso a todas as aplicações.</p>
 *
 * <p>Esta é a fonte única do mapeamento aplicação -> roles, usada tanto pelo
 * serviço de usuários (validação no login) quanto pelo gateway (validação por
 * rota).</p>
 */
public enum AppScope {
    ADMIN("ADMIN"),
    FINANCES("FINANCES"),
    BUDGETS("BUDGETS"),
    STUDY("STUDY"),
    FITNESS("HEALTH_USER", "HEALTH_PERSONAL", "HEALTH_NUTRITIONIST");

    public static final String ADMIN_ROLE = "ADMIN";

    private final Set<String> requiredRoles;

    AppScope(String... requiredRoles) {
        this.requiredRoles = Set.of(requiredRoles);
    }

    public Set<String> requiredRoles() {
        return requiredRoles;
    }

    /**
     * Indica se a coleção de roles dá acesso a esta aplicação.
     */
    public boolean allows(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        if (roles.contains(ADMIN_ROLE)) {
            return true;
        }
        return roles.stream().anyMatch(requiredRoles::contains);
    }

    /**
     * Resolve a aplicação a partir do nome do serviço roteado no gateway
     * (ex.: {@code finances}, {@code budgets}).
     */
    public static Optional<AppScope> fromService(String service) {
        if (service == null || service.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(app -> app.name().equalsIgnoreCase(service))
                .findFirst();
    }

    /**
     * Resolve a aplicação a partir do nome informado pelo cliente
     * (ex.: {@code finances} ou {@code FINANCES}).
     */
    public static Optional<AppScope> fromName(String name) {
        return fromService(name);
    }

    /**
     * Resolve a aplicação dona de uma role (ex.: {@code FINANCES} ->
     * {@code AppScope.FINANCES}, {@code HEALTH_USER} -> {@code AppScope.FITNESS}).
     * A role {@code ADMIN} não pertence a nenhuma aplicação.
     */
    public static Optional<AppScope> fromRole(String role) {
        if (role == null || role.isBlank() || ADMIN_ROLE.equalsIgnoreCase(role)) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(app -> app.requiredRoles.contains(role))
                .findFirst();
    }

    /**
     * Lista as aplicações acessíveis para a coleção de roles informada.
     */
    public static List<AppScope> appsFor(Collection<String> roles) {
        return Arrays.stream(values())
                .filter(app -> app.allows(roles))
                .toList();
    }
}
