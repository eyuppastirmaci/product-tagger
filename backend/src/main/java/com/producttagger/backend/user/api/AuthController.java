package com.producttagger.backend.user.api;

import com.producttagger.backend.shared.api.ClientIp;
import com.producttagger.backend.shared.security.AuthCookies;
import com.producttagger.backend.shared.security.AuthenticatedUser;
import com.producttagger.backend.shared.security.JwtService;
import com.producttagger.backend.user.application.AuthService;
import com.producttagger.backend.user.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final AuthCookies authCookies;

    AuthController(AuthService authService, JwtService jwtService, AuthCookies authCookies) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.authCookies = authCookies;
    }

    @PostMapping("/register")
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.name(), request.email(), request.password());

        // Registration logs the user in immediately
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, sessionCookie(user))
                .body(UserResponse.from(user));
    }

    @PostMapping("/login")
    ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request, @ClientIp String clientIp) {
        User user = authService.authenticate(request.email(), request.password(), clientIp);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie(user))
                .body(UserResponse.from(user));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookies.expired().toString())
                .build();
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        // Fresh read so the response reflects any profile change since the token was issued
        return UserResponse.from(authService.byId(principal.id()));
    }

    private String sessionCookie(User user) {
        String token = jwtService.issue(new AuthenticatedUser(user.getId(), user.getEmail(), user.getName()));

        return authCookies.session(token).toString();
    }
}
