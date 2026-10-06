package com.skafferi.controller;

import com.skafferi.domain.User;
import com.skafferi.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getProviders(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            origin = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        }
        return ResponseEntity.ok(authService.getAvailableProviders(origin));
    }

    @GetMapping("/callback/oidc")
    public void handleOidcCallback(@RequestParam String code,
                                  HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {
        String redirectUri = request.getRequestURL().toString();
        User user = authService.handleOidcCallback(code, redirectUri);
        // Redirect back to frontend home with user info
        response.sendRedirect("/?auth_user=" + user.getId());
    }

    @GetMapping("/callback/google")
    public void handleGoogleCallback(@RequestParam String code,
                                    HttpServletRequest request,
                                    HttpServletResponse response) throws IOException {
        String redirectUri = request.getRequestURL().toString();
        User user = authService.handleGoogleCallback(code, redirectUri);
        // Redirect back to frontend home with user info
        response.sendRedirect("/?auth_user=" + user.getId());
    }
}
