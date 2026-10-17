package com.example.demo.service;

import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, JwtService jwtService, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    public AuthResponse login(AuthRequest request) {
        String tenantId = UserService.resolveTenant(request.tenantId());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsernameAndTenantId(request.username(), tenantId)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or tenant"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getTenantId());
    }

    public UserResponse register(RegisterRequest request) {
        return userService.createUser(request);
    }
}
