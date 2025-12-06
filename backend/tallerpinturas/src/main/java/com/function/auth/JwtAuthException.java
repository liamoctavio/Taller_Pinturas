package com.function.auth;

/**
 * Excepción única para errores del módulo de validación JWT.
 * Es RuntimeException para no forzar catches en toda la app.
 */
public class JwtAuthException extends RuntimeException {
  public JwtAuthException(String message) {
    super(message);
  }
  public JwtAuthException(String message, Throwable cause) {
    super(message, cause);
  }
}
