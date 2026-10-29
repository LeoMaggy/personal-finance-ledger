package com.mahesh.expenses.user;

import com.mahesh.expenses.common.CurrentUser;
import org.springframework.stereotype.Component;

@Component
public class DemoCurrentUser implements CurrentUser {

    private static final String DEMO_EMAIL = "demo@expenses.local";

    private final UserRepository users;

    public DemoCurrentUser(UserRepository users) {
        this.users = users;
    }

    @Override
    public Long id() {
        return users.findByEmail(DEMO_EMAIL)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Demo user missing; did Flyway V2 run?"));
    }
}
