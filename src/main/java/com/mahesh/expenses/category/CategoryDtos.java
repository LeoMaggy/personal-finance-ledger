package com.mahesh.expenses.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {
    private CategoryDtos() {
    }

    public record CreateCategoryRequest(
            @NotBlank @Size(max = 50)
            String name,

            @NotNull
            CategoryType type) {
    }

    public record CategoryResponse(Long id, String name, CategoryType type) {
        public static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getType());
        }
    }
}
