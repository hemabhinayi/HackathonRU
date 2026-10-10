package com.sayless.config;

import java.net.URI;
import java.time.Duration;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Validation runs only when enabled, and failure messages never include the key or other values,
 * so a misconfigured startup cannot print the credential.
 */
@ConfigurationProperties(prefix = "sayless.ai")
public record AiProperties(
        boolean enabled,
        String apiKey,
        String model,
        URI baseUrl,
        Duration connectTimeout,
        Duration requestTimeout) {

    static final Duration MAX_REQUEST_TIMEOUT = Duration.ofSeconds(25);
    private static final Pattern MODEL_ID = Pattern.compile("[A-Za-z0-9._-]+");

    public AiProperties {
        if (enabled) {
            validateEnabled(apiKey, model, baseUrl, connectTimeout, requestTimeout);
        }
    }

    private static void validateEnabled(String apiKey, String model, URI baseUrl,
            Duration connectTimeout, Duration requestTimeout) {
        if (apiKey == null || apiKey.isBlank()) {
            throw invalid("sayless.ai.api-key (GEMINI_API_KEY) must be set when sayless.ai.enabled=true");
        }
        if (model == null || !MODEL_ID.matcher(model).matches()) {
            throw invalid("sayless.ai.model (GEMINI_MODEL) must be a plain model id such as gemini-3.5-flash-lite");
        }
        validateBaseUrl(baseUrl);
        if (connectTimeout == null || requestTimeout == null
                || connectTimeout.isNegative() || connectTimeout.isZero()
                || requestTimeout.isNegative() || requestTimeout.isZero()) {
            throw invalid("sayless.ai connect-timeout and request-timeout must be positive durations");
        }
        if (connectTimeout.compareTo(requestTimeout) > 0) {
            throw invalid("sayless.ai.connect-timeout must not exceed sayless.ai.request-timeout");
        }
        if (requestTimeout.compareTo(MAX_REQUEST_TIMEOUT) > 0) {
            throw invalid("sayless.ai.request-timeout must be at most 25s");
        }
    }

    private static void validateBaseUrl(URI baseUrl) {
        String message = "sayless.ai.base-url (GEMINI_BASE_URL) must be an absolute https URL"
                + " without query, fragment or credentials (http is allowed only for loopback test servers)";
        if (baseUrl == null || !baseUrl.isAbsolute() || baseUrl.getHost() == null
                || baseUrl.getRawQuery() != null || baseUrl.getRawFragment() != null
                || baseUrl.getRawUserInfo() != null) {
            throw invalid(message);
        }
        String scheme = baseUrl.getScheme().toLowerCase(java.util.Locale.ROOT);
        boolean https = scheme.equals("https");
        boolean loopbackHttp = scheme.equals("http") && isLoopback(baseUrl.getHost());
        if (!https && !loopbackHttp) {
            throw invalid(message);
        }
    }

    private static boolean isLoopback(String host) {
        return host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("[::1]");
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    @Override
    public String toString() {
        return "AiProperties[enabled=" + enabled
                + ", apiKey=" + (apiKey == null || apiKey.isEmpty() ? "<empty>" : "<redacted>")
                + ", model=" + model
                + ", baseUrl=" + baseUrl
                + ", connectTimeout=" + connectTimeout
                + ", requestTimeout=" + requestTimeout + "]";
    }
}
