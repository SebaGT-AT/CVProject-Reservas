package cl.reservas.auth;

import jakarta.servlet.http.HttpServletRequest;

public record ClientRequestInfo(String ipAddress, String userAgent) {
    public static ClientRequestInfo from(HttpServletRequest request) {
        return new ClientRequestInfo(limit(request.getRemoteAddr(), 64), limit(request.getHeader("User-Agent"), 500));
    }

    private static String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
