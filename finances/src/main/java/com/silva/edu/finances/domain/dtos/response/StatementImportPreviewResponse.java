package com.silva.edu.finances.domain.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StatementImportPreviewResponse(List<Line> lines, int importable, int duplicates) {
    public record Line(int row, String key, String description, BigDecimal amount, boolean income,
                       LocalDate date, boolean duplicate) {}
}
