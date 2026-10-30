package com.mahesh.expenses.auth;

import com.mahesh.expenses.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {
    private AuthDtos(){
    }

    public record SignupRequest(
            @NotBlank @Email @Size(max=255) String email,
            @NotBlank @Size(min=8, max=72, message="must be 8 to 72 characters") String password,
            @NotBlank @Size(max=100) String fullName){
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank @Size(max=255) String password){}

    public record TokenResponse(String accessToken, String tokenType, long expiresOn){
    }

    /** Never includes the password hash. */
    public record UserResponse(Long id, String email, String fullName, Instant createdAt){
        public static UserResponse from(User user){
            return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getCreatedAt());
        }
    }
}
