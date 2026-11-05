package com.silva.edu.finances.repositories;

import com.silva.edu.finances.domain.entities.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {}
