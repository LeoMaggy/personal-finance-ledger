package com.mahesh.expenses.category;

import com.mahesh.expenses.common.CurrentUser;
import com.mahesh.expenses.common.NotFoundException;
import com.mahesh.expenses.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mahesh.expenses.category.CategoryDtos.*;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository users;
    private final CurrentUser currentUser;

    public CategoryService(CategoryRepository categoryRepository, UserRepository users, CurrentUser currentUser) {
        this.categoryRepository = categoryRepository;
        this.users = users;
        this.currentUser = currentUser;
    }

    public List<CategoryResponse> list(){
        return categoryRepository.findByUserIdOrderByTypeAscNameAsc(currentUser.id())
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse create(CreateCategoryRequest request) {
        Long userId = currentUser.id();
        String name = request.name().strip();
        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, name, request.type())) {
            throw new IllegalArgumentException("Category with name '%s' already exists".formatted(name));
        }
        Category category = new Category(users.getReferenceById(userId), name, request.type());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public Category getOwnedCategory(Long id) {
        return categoryRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Category %d not found".formatted(id)));
    }
}
