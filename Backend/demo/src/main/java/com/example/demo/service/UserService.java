package com.example.demo.service;

import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(RegisterRequest request) {
        String tenantId = resolveTenant(request.tenantId());
        String username = request.username() == null ? "" : request.username().trim();
        if (username.isBlank() || request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }
        if (userRepository.findByUsernameAndTenantId(username, tenantId).isPresent()) {
            throw new IllegalArgumentException("User already exists in this tenant");
        }

        Role role = Role.valueOf(request.role().trim().toUpperCase(Locale.ROOT));
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setTenantId(tenantId);
        userRepository.save(user);

        return toResponse(user);
    }

    public UserResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("No authenticated user");
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toResponse(user);
    }

    public List<UserResponse> getUsersForCurrentTenant() {
        String tenantId = resolveTenant(null);
        return userRepository.findByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    public User findUserByUsername(String username) {
        String tenantId = resolveTenant(null);
        return userRepository.findByUsernameAndTenantId(username, tenantId)
                .orElseGet(() -> userRepository.findByUsername(username)
                        .orElseThrow(() -> new IllegalArgumentException("User not found")));
    }

    public static String resolveTenant(String requestTenantId) {
        String tenantId = requestTenantId != null ? requestTenantId.trim() : TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant is required");
        }
        return tenantId;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole().name(), user.getTenantId());
    }
}
