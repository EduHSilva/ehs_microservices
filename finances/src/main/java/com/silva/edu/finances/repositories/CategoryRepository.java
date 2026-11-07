package com.silva.edu.finances.repositories;

import com.silva.edu.finances.domain.entities.Category;
import com.silva.edu.finances.domain.enums.CategoryClassification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByIdAndUserId(UUID id, UUID userId);
    Page<Category> findAllByUserId(UUID userId, Pageable pageable);
    Optional<Category> findFirstByUserIdAndClassification(UUID userId, CategoryClassification classification);

    @Query(value = "select id, title, income, status, default_key from category where user_id = :userId", nativeQuery = true)
    List<Object[]> findSeedStateByUserId(@Param("userId") UUID userId);
}
