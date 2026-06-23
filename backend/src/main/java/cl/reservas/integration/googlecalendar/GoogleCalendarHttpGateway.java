package cl.reservas.integration.googlecalendar;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
class GoogleCalendarHttpGateway implements GoogleCalendarGateway {
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String REVOKE_URL = "https://oauth2.googleapis.com/revoke";
    private static final String CALENDAR_API = "https://www.googleapis.com/calendar/v3";
    private final RestClient restClient;
    private final GoogleCalendarProperties properties;

    GoogleCalendarHttpGateway(RestClient.Builder builder, GoogleCalendarProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    @Override
    public GoogleTokenResponse exchangeAuthorizationCode(String code) {
        var form = form();
        form.add("code", code);
        form.add("redirect_uri", properties.redirectUri());
        form.add("grant_type", "authorization_code");
        try {
            GoogleTokenResponse response = restClient.post().uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                    .retrieve().body(GoogleTokenResponse.class);
            if (response == null || response.refreshToken() == null || response.refreshToken().isBlank()) {
                throw new GoogleAuthorizationException("Google no entrego un refresh token; vuelve a autorizar la cuenta");
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw new GoogleAuthorizationException("Google rechazo el codigo de autorizacion", exception);
        }
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        var form = form();
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");
        try {
            GoogleTokenResponse response = restClient.post().uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                    .retrieve().body(GoogleTokenResponse.class);
            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new GoogleAuthorizationException("Google no entrego un access token");
            }
            return response.accessToken();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw new GoogleAuthorizationException("La autorizacion de Google expiro o fue revocada", exception);
            }
            throw exception;
        }
    }

    @Override
    public void upsertEvent(String accessToken, String calendarId, CalendarSyncPayload payload) {
        String collectionUri = eventsUri(calendarId);
        Map<String, Object> body = eventBody(payload);
        try {
            restClient.post().uri(uriWithNoNotifications(collectionUri))
                    .headers(headers -> headers.setBearerAuth(accessToken)).body(body).retrieve().toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 409) throw exception;
            restClient.put().uri(uriWithNoNotifications(collectionUri + "/" + payload.eventId()))
                    .headers(headers -> headers.setBearerAuth(accessToken)).body(body).retrieve().toBodilessEntity();
        }
    }

    @Override
    public void deleteEvent(String accessToken, String calendarId, String eventId) {
        try {
            restClient.delete().uri(uriWithNoNotifications(eventsUri(calendarId) + "/" + eventId))
                    .headers(headers -> headers.setBearerAuth(accessToken)).retrieve().toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404 && exception.getStatusCode().value() != 410) throw exception;
        }
    }

    @Override
    public void revoke(String refreshToken) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("token", refreshToken);
        try {
            restClient.post().uri(REVOKE_URL).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().toBodilessEntity();
        } catch (RestClientException ignored) {
            // La desconexion local debe completarse incluso si Google ya revoco el token.
        }
    }

    private LinkedMultiValueMap<String, String> form() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        return form;
    }

    private String eventsUri(String calendarId) {
        return UriComponentsBuilder.fromUriString(CALENDAR_API)
                .pathSegment("calendars", calendarId, "events").build().encode().toUriString();
    }

    private String uriWithNoNotifications(String uri) {
        return UriComponentsBuilder.fromUriString(uri).queryParam("sendUpdates", "none")
                .build().encode().toUriString();
    }

    private Map<String, Object> eventBody(CalendarSyncPayload payload) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", payload.eventId());
        body.put("summary", payload.summary());
        body.put("description", payload.description());
        body.put("start", Map.of("dateTime", payload.startAt().toString(), "timeZone", payload.timeZone()));
        body.put("end", Map.of("dateTime", payload.endAt().toString(), "timeZone", payload.timeZone()));
        body.put("extendedProperties", Map.of("private", Map.of(
                "reservasAppointmentId", payload.appointmentId().toString())));
        return body;
    }
}
