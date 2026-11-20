package com.mahesh.expenses.category;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.mahesh.expenses.category.CategoryDtos.*;

import java.util.List;

@Tag(name = "Categories")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoriesController {

    private final CategoryService categoryService;

    public CategoriesController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> listCategories() {
        return categoryService.list();
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.create(request);
    }
}
