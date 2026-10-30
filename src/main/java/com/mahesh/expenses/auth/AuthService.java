package com.mahesh.expenses.auth;

import com.mahesh.expenses.audit.AuditAction;
import com.mahesh.expenses.audit.AuditService;
import com.mahesh.expenses.auth.AuthDtos.*;
import com.mahesh.expenses.category.CategoryService;
import com.mahesh.expenses.common.*;
import com.mahesh.expenses.security.TokenService;
import com.mahesh.expenses.user.User;
import com.mahesh.expenses.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository users;
    private final CategoryService categoryService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuditService auditService;
    private final CurrentUser currentUser;
    private final String dummyHash;

    public AuthService(UserRepository user, CategoryService category, PasswordEncoder passwordEncoder, TokenService tokenService, AuditService auditService, CurrentUser currentUser) {
        this.users = user;
        this.categoryService = category;
        this.auditService = auditService;
        this.tokenService = tokenService;
        this.currentUser = currentUser;
        this.passwordEncoder = passwordEncoder;
        //used when email doesnt exist, so a wrong email takes as long as a wrong password
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public UserResponse signUp(SignupRequest signupRequest) {
        String email = normalise(signupRequest.email());
        if(users.existsByEmail(email)){
            throw new ConflictException("An account with this email already exists.");
        }
        User user = users.save(new User(email, signupRequest.fullName().strip(), passwordEncoder.encode(signupRequest.password())));
        categoryService.createDefaultCategories(user);
        auditService.recordFor(user.getId(), AuditAction.USER_SIGNED_UP, EntityType.USER.name(), user.getId(), null);
        return UserResponse.from(user);
    }

    /** Not @Transactional: each audit record commits on its own, whatever the outcome. */
    public TokenResponse login(LoginRequest request) {
        String email = normalise(request.email());
        Optional<User> user = users.findByEmail(email);

        // Always run BCrypt, even for an unknown email, so response times don't reveal which emails exist
        String hash = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        if (user.isEmpty() || !passwordMatches) {
            Long userId = user.map(User::getId).orElse(null);
            auditService.recordInNewTransaction(userId, AuditAction.LOGIN_FAILED, "USER", userId, "email=" + email);
            throw new InvalidCredentialsException();
        }

        User found = user.get();
        auditService.recordInNewTransaction(found.getId(), AuditAction.LOGIN_SUCCEEDED, "USER", found.getId(), null);
        TokenService.IssuedToken token = tokenService.issue(found);
        return new TokenResponse(token.value(), "Bearer", token.expiresAt());
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return users.findById(currentUser.id())
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private static String normalise(String email){
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
