package com.silva.edu.finances.domain.dtos.response;

import java.math.BigDecimal;

public record InvestmentSummaryResponse(String accountId, BigDecimal totalContributions,
                                        BigDecimal totalWithdrawals, BigDecimal balance, String currency) {
}
