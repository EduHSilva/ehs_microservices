package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.infra.config.UserContext;
import com.silva.edu.finances.repositories.CategoryRepository;
import com.silva.edu.finances.services.FinanceAccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {
    @Mock private CategoryRepository repository;
    @Mock private CategoryDefaultSeeder defaultSeeder;
    @Mock private FinanceAccountService accountService;
    @InjectMocks private CategoryServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    @AfterEach
    void clearUser() {
        UserContext.clear();
    }

    @Test
    void findAllOrdersIncomeFirstThenGoalDescendingThenTitle() {
        UserContext.setUser(userId);
        when(defaultSeeder.needsSeed(userId)).thenReturn(false);
        when(accountService.visibleUserIds()).thenReturn(Set.of(userId));
        when(repository.findAllByUserIdIn(any(), any())).thenReturn(Page.empty());

        service.findAll(0, 50);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByUserIdIn(any(), captor.capture());
        Sort sort = captor.getValue().getSort();
        assertThat(sort.getOrderFor("income").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sort.getOrderFor("goal").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sort.getOrderFor("title").getDirection()).isEqualTo(Sort.Direction.ASC);
    }
}
