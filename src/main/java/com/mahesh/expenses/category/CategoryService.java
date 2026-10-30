package com.mahesh.expenses.category;

import com.mahesh.expenses.audit.AuditAction;
import com.mahesh.expenses.audit.AuditService;
import com.mahesh.expenses.common.CurrentUser;
import com.mahesh.expenses.common.EntityType;
import com.mahesh.expenses.common.NotFoundException;
import com.mahesh.expenses.user.User;
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
    private final AuditService auditService;

    public CategoryService(CategoryRepository categoryRepository, UserRepository users, CurrentUser currentUser, AuditService auditService) {
        this.categoryRepository = categoryRepository;
        this.users = users;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    public List<CategoryResponse> list(){
        return categoryRepository.findByUserIdOrderByTypeAscNameAsc(currentUser.id())
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        Long userId = currentUser.id();
        String name = request.name().strip();
        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, name, request.type())) {
            throw new IllegalArgumentException("Category with name '%s' already exists".formatted(name));
        }
        Category category = categoryRepository.save(new Category(users.getReferenceById(userId), name, request.type()));
        auditService.record(AuditAction.CATEGORY_CREATED, EntityType.CATEGORY.name(), category.getId(),
                "%s category '%s'".formatted(category.getType(), name));
        return CategoryResponse.from(category);
    }

    public Category getOwnedCategory(Long id) {
        return categoryRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Category %d not found".formatted(id)));
    }

    //called at signUp, so every new user will have same setup of categories
    @Transactional
    public void createDefaultCategories(User user){
        categoryRepository.saveAll(List.of(
                new Category(user, "Food", CategoryType.EXPENSE),
                new Category(user, "Rent", CategoryType.EXPENSE),
                new Category(user, "Transport", CategoryType.EXPENSE),
                new Category(user, "Utilities", CategoryType.EXPENSE),
                new Category(user, "Subscriptions", CategoryType.EXPENSE),
                new Category(user, "Shopping", CategoryType.EXPENSE),
                new Category(user, "Health", CategoryType.EXPENSE),
                new Category(user, "Salary", CategoryType.INCOME),
                new Category(user, "Interest", CategoryType.INCOME)));
    }
}
