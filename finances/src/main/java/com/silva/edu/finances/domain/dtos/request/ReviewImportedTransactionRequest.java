package com.silva.edu.finances.domain.dtos.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReviewImportedTransactionRequest(@NotNull Boolean approved, UUID categoryId) {}
