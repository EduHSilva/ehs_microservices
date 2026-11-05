package com.silva.edu.finances.domain.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record StatementImportRequest(
        @NotBlank @Size(max = 80) String accountId,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,40}") String source,
        @NotEmpty @Size(max = 1000) List<@Valid StatementImportLineRequest> lines
) {}
