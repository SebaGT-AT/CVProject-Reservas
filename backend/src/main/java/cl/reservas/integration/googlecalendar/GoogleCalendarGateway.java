package cl.reservas.integration.googlecalendar;

interface GoogleCalendarGateway {
    GoogleTokenResponse exchangeAuthorizationCode(String code);
    String refreshAccessToken(String refreshToken);
    void upsertEvent(String accessToken, String calendarId, CalendarSyncPayload payload);
    void deleteEvent(String accessToken, String calendarId, String eventId);
    void revoke(String refreshToken);
}
