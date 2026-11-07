package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.catalog.DefaultCategories;
import com.silva.edu.finances.domain.catalog.DefaultCategories.DefaultCategory;
import com.silva.edu.finances.domain.entities.Category;
import com.silva.edu.finances.domain.enums.CategoryStatus;
import com.silva.edu.finances.repositories.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CategoryDefaultSeeder {
    private static final Object SEED_LOCK = new Object();

    private final CategoryRepository repository;
    private final TransactionTemplate transactionTemplate;

    public CategoryDefaultSeeder(CategoryRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public boolean needsSeed(UUID userId) {
        Set<String> knownKeys = new HashSet<>();
        for (Object[] row : repository.findSeedStateByUserId(userId)) {
            if (row[4] != null) knownKeys.add((String) row[4]);
        }
        return DefaultCategories.ALL.stream().anyMatch(entry -> !knownKeys.contains(entry.key()));
    }

    public void seed(UUID userId) {
        if (!needsSeed(userId)) return;
        synchronized (SEED_LOCK) {
            transactionTemplate.executeWithoutResult(status -> seedInTransaction(userId));
        }
    }

    private void seedInTransaction(UUID userId) {
        if (!needsSeed(userId)) return;

        Set<String> knownKeys = new HashSet<>();
        Map<String, UUID> activeByTitleAndDirection = new HashMap<>();
        List<Object[]> rows = repository.findSeedStateByUserId(userId);
        for (Object[] row : rows) {
            String title = (String) row[1];
            boolean income = Boolean.TRUE.equals(row[2]);
            String status = (String) row[3];
            String defaultKey = (String) row[4];
            if (defaultKey != null) knownKeys.add(defaultKey);
            else if (CategoryStatus.ACTIVE.name().equals(status) && title != null) {
                activeByTitleAndDirection.put(seedSignature(title, income), (UUID) row[0]);
            }
        }

        for (DefaultCategory entry : DefaultCategories.ALL) {
            if (knownKeys.contains(entry.key())) continue;
            UUID existingId = activeByTitleAndDirection.get(seedSignature(entry.title(), entry.income()));
            if (existingId != null) {
                repository.findById(existingId).ifPresent(existing -> {
                    existing.setDefaultKey(entry.key());
                    repository.save(existing);
                });
            } else {
                repository.save(Category.fromDefault(entry, userId));
            }
            knownKeys.add(entry.key());
        }
    }

    private String seedSignature(String title, boolean income) {
        String normalized = Normalizer.normalize(title.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized + "|" + income;
    }
}
