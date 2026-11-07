package com.silva.edu.finances.domain.catalog;

import com.silva.edu.finances.domain.enums.CategoryClassification;

import java.util.List;
import java.util.Optional;

public final class DefaultCategories {
    public static final String INVESTMENT_KEY = "defaultCategory.investment";

    public record DefaultCategory(String key, String title, boolean income, String color,
                                  CategoryClassification classification) {}

    public static final List<DefaultCategory> ALL = List.of(
            new DefaultCategory("defaultCategory.salary", "Salário", true, "#16A34A", CategoryClassification.ESSENTIAL),
            new DefaultCategory("defaultCategory.freelance", "Freelance", true, "#0D9488", CategoryClassification.ESSENTIAL),
            new DefaultCategory("defaultCategory.sales", "Vendas", true, "#0891B2", CategoryClassification.ESSENTIAL),
            new DefaultCategory("defaultCategory.otherIncome", "Outras receitas", true, "#65A30D", CategoryClassification.ESSENTIAL),
            new DefaultCategory("defaultCategory.housing", "Moradia", false, "#7C3AED", CategoryClassification.ESSENTIAL),
            new DefaultCategory("defaultCategory.food", "Alimentação", false, "#EA580C", CategoryClassification.ESSENTIAL),
            new DefaultCategory("defaultCategory.transport", "Transporte", false, "#0284C7", CategoryClassification.ESSENTIAL),
            new DefaultCategory("defaultCategory.health", "Saúde", false, "#DC2626", CategoryClassification.ESSENTIAL),
            new DefaultCategory("defaultCategory.education", "Educação", false, "#4F46E5", CategoryClassification.ESSENTIAL),
            new DefaultCategory("defaultCategory.utilities", "Contas e utilidades", false, "#B45309", CategoryClassification.ESSENTIAL),
            new DefaultCategory("defaultCategory.subscriptions", "Assinaturas", false, "#9333EA", CategoryClassification.RECREATION),
            new DefaultCategory("defaultCategory.leisure", "Lazer", false, "#DB2777", CategoryClassification.RECREATION),
            new DefaultCategory("defaultCategory.shopping", "Compras", false, "#C026D3", CategoryClassification.RECREATION),
            new DefaultCategory("defaultCategory.taxes", "Impostos", false, "#475569", CategoryClassification.ESSENTIAL),
            new DefaultCategory(INVESTMENT_KEY, "Investimentos", false, "#2563EB", CategoryClassification.INVESTMENT),
            new DefaultCategory("defaultCategory.other", "Outros", false, "#64748B", CategoryClassification.ESSENTIAL)
    );

    public static Optional<DefaultCategory> findByKey(String key) {
        return ALL.stream().filter(category -> category.key().equals(key)).findFirst();
    }

    private DefaultCategories() {
    }
}
