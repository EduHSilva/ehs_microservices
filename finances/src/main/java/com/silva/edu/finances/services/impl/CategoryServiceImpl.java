package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.catalog.DefaultCategories;
import com.silva.edu.finances.domain.catalog.DefaultCategories.DefaultCategory;
import com.silva.edu.finances.domain.dtos.request.AddCategoryRequestDTO;
import com.silva.edu.finances.domain.dtos.request.UpdateCategoryRequestDTO;
import com.silva.edu.finances.domain.dtos.response.CategoryResponseDTO;
import com.silva.edu.finances.domain.entities.Category;
import com.silva.edu.finances.domain.enums.CategoryClassification;
import com.silva.edu.finances.domain.enums.CategoryStatus;
import com.silva.edu.finances.infra.exceptions.CustomExceptions;
import com.silva.edu.finances.repositories.CategoryRepository;
import com.silva.edu.finances.services.CategoryService;
import com.silva.edu.finances.infra.config.UserContext;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repository;
    private final CategoryDefaultSeeder defaultSeeder;

    public CategoryServiceImpl(CategoryRepository repository, CategoryDefaultSeeder defaultSeeder) {
        this.repository = repository;
        this.defaultSeeder = defaultSeeder;
    }

    @Override
    public CategoryResponseDTO save(AddCategoryRequestDTO request) {
        Category category = new Category();
        category.setTitle(request.title());
        category.setColor(request.color());
        category.setIncome(Boolean.TRUE.equals(request.income()));
        category.setGoal(category.isIncome() ? 0 : request.goal());
        category.setClassification(request.classification() == null
                ? CategoryClassification.ESSENTIAL : request.classification());
        category.setStatus(CategoryStatus.ACTIVE);
        category.setUserId(UserContext.getUser());
        repository.save(category);
        return new CategoryResponseDTO(category);
    }

    @Override
    public void delete(UUID id) {
        repository.delete(findEntity(id));
    }

    @Override
    public Page<@NonNull CategoryResponseDTO> findAll(int page, int size) {
        seedDefaults(UserContext.getUser());
        Pageable pageable = PageRequest.of(page, size);
        return repository.findAllByUserId(UserContext.getUser(), pageable).map(CategoryResponseDTO::new);
    }

    @Override
    public Category ensureInvestmentCategory(UUID userId) {
        seedDefaults(userId);
        return repository.findFirstByUserIdAndClassification(userId, CategoryClassification.INVESTMENT)
                .orElseGet(() -> {
                    DefaultCategory entry = DefaultCategories.findByKey(DefaultCategories.INVESTMENT_KEY)
                            .orElseThrow();
                    Category category = Category.fromDefault(entry, userId);
                    category.setDefaultKey(null);
                    return repository.save(category);
                });
    }

    @Override
    public CategoryResponseDTO findById(UUID id) {
        return new CategoryResponseDTO(findEntity(id));
    }

    @Override
    public CategoryResponseDTO update(UUID id, UpdateCategoryRequestDTO request) {
        Category category = findEntity(id);
        if (!request.title().equals(category.getTitle())) {
            category.setCustomized(category.getDefaultKey() != null && !isCatalogTitle(category.getDefaultKey(), request.title()));
        }
        category.setTitle(request.title());
        category.setColor(request.color());
        if (request.income() != null) category.setIncome(request.income());
        category.setGoal(category.isIncome() ? 0 : request.goal());
        if (request.classification() != null) category.setClassification(request.classification());
        else if (category.getClassification() == null) category.setClassification(CategoryClassification.ESSENTIAL);
        return new CategoryResponseDTO(repository.save(category));
    }

    private void seedDefaults(UUID userId) {
        if (!defaultSeeder.needsSeed(userId)) return;
        defaultSeeder.seed(userId);
    }

    private boolean isCatalogTitle(String defaultKey, String title) {
        return DefaultCategories.findByKey(defaultKey)
                .map(entry -> entry.title().equalsIgnoreCase(title.trim()))
                .orElse(false);
    }

    private Category findEntity(UUID id) {
        return repository.findByIdAndUserId(id, UserContext.getUser())
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Category.class.getSimpleName(), id));
    }
}
