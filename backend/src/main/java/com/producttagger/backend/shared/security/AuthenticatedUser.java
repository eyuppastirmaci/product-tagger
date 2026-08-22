package com.producttagger.backend.shared.security;

/**
 * The security principal, built entirely from JWT claims; no database hit per
 * request.
 */
public record AuthenticatedUser(Long id, String email, String name) {
}
