package org.azdev.barber_book.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class EvolutionApiClient {

    private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String apiKey;

    public EvolutionApiClient(
            @Value("${evolution.api.url}") String apiUrl,
            @Value("${evolution.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(normalizeUrl(apiUrl))
                .defaultHeader("apikey", apiKey)
                .build();
    }

    private static String normalizeUrl(String url) {
        String value = url == null ? "" : url.trim();
        if (!value.matches("(?i)^https?://.*")) {
            value = "https://" + value;
        }
        return value.replaceAll("/+$", "");
    }

    public String createInstance(String instanceName) {
        ensureConfigured();
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/instance/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "instanceName", instanceName,
                            "qrcode", true,
                            "integration", "WHATSAPP-BAILEYS"
                    ))
                    .retrieve()
                    .body(RESPONSE_TYPE);

            return getQrCode(response);
        } catch (HttpClientErrorException.Forbidden exception) {
            if (!isInstanceAlreadyInUse(exception.getResponseBodyAsString(), instanceName)) {
                throw exception;
            }
            return connectInstance(instanceName);
        }
    }

    public String connectInstance(String instanceName) {
        ensureConfigured();
        Map<String, Object> response = restClient.get()
                .uri("/instance/connect/{instanceName}", instanceName)
                .retrieve()
                .body(RESPONSE_TYPE);

        return getQrCode(response);
    }

    public boolean isInstanceConnected(String instanceName) {
        ensureConfigured();
        Map<String, Object> response = restClient.get()
                .uri("/instance/connectionState/{instanceName}", instanceName)
                .retrieve()
                .body(RESPONSE_TYPE);

        Object instanceValue = response == null ? null : response.get("instance");
        if (!(instanceValue instanceof Map<?, ?> instance)) {
            throw new IllegalStateException("A Evolution API retornou um estado de conexão inválido");
        }
        Object state = instance.get("state");
        if (!(state instanceof String stateValue)) {
            throw new IllegalStateException("A Evolution API retornou um estado de conexão inválido");
        }
        return "open".equalsIgnoreCase(stateValue);
    }

    public void disconnectInstance(String instanceName) {
        ensureConfigured();
        restClient.delete()
                .uri("/instance/logout/{instanceName}", instanceName)
                .retrieve()
                .toBodilessEntity();
    }

    public void sendText(String instanceName, String number, String message) {
        ensureConfigured();
        restClient.post()
                .uri("/message/sendText/{instanceName}", instanceName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("number", number, "text", message))
                .retrieve()
                .toBodilessEntity();
    }

    private String getQrCode(Map<String, Object> response) {
        Object qrCodeValue = response == null ? null : response.get("qrcode");
        Object base64Value = qrCodeValue instanceof Map<?, ?> qrCode
                ? qrCode.get("base64")
                : response == null ? null : response.get("base64");
        if (!(base64Value instanceof String base64) || base64.isBlank()) {
            throw new IllegalStateException("A Evolution API não retornou o QR code esperado");
        }
        return base64;
    }

    private boolean isInstanceAlreadyInUse(String responseBody, String instanceName) {
        String normalizedBody = responseBody.replace("\\\"", "\"");
        return normalizedBody.contains("This name \"" + instanceName + "\" is already in use.");
    }

    private void ensureConfigured() {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("EVOLUTION_API_KEY não está configurada");
        }
    }
}
