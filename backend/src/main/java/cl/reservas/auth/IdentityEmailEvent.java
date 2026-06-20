package cl.reservas.auth;

record IdentityEmailEvent(String email, String name, String token, OneTimeTokenType type) {}

