package com.sayless.provider;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import com.sayless.config.AiProperties;
import com.sayless.contract.AiRewriteClient;
import com.sayless.contract.ErrorCode;
import com.sayless.contract.RewriteException;
import com.sayless.contract.RewriteLimits;
import com.sayless.contract.Tone;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Gemini Developer API generateContent client. Count, blank, length and duplicate rules are
 * enforced by the rewrite service, not here; this class only validates the provider envelope and
 * JSON types. Nothing from requests or provider responses is logged.
 */
@Component
@ConditionalOnBooleanProperty("sayless.ai.enabled")
public class GeminiRewriteClient implements AiRewriteClient {

    static final String PROMPT_RESOURCE = "prompts/rewrite-system.txt";

    private static final Set<String> REFUSAL_FINISH_REASONS = Set.of(
            "SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "IMAGE_SAFETY");
    private static final Set<String> PART_METADATA_FIELDS = Set.of("thought", "thoughtSignature");

    private final JsonMapper json = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();
    private final HttpClient httpClient;
    private final URI endpoint;
    private final String apiKey;
    private final Duration requestTimeout;
    private final String systemPrompt;

    @Autowired
    public GeminiRewriteClient(AiProperties properties) {
        this(properties, HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    GeminiRewriteClient(AiProperties properties, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.apiKey = properties.apiKey();
        this.requestTimeout = properties.requestTimeout();
        this.endpoint = endpointFor(properties.baseUrl(), properties.model());
        this.systemPrompt = loadPrompt();
    }

    @Override
    public List<String> generate(String normalizedMessage, Tone tone) {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(normalizedMessage, tone), UTF_8))
                .build();

        HttpResponse<String> response = send(request);
        int status = response.statusCode();
        if (status == 429) {
            throw new RewriteException(ErrorCode.AI_RATE_LIMITED);
        }
        if (status < 200 || status >= 300) {
            throw new RewriteException(ErrorCode.AI_UNAVAILABLE);
        }
        return parseResponse(response.body());
    }

    /** The deadline covers the whole exchange, including the response body, not just the headers. */
    private HttpResponse<String> send(HttpRequest request) {
        CompletableFuture<HttpResponse<String>> future =
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(UTF_8));
        try {
            return future.get(requestTimeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RewriteException(ErrorCode.AI_TIMEOUT);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new RewriteException(ErrorCode.AI_UNAVAILABLE);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            while (cause instanceof CompletionException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof HttpTimeoutException) {
                throw new RewriteException(ErrorCode.AI_TIMEOUT);
            }
            if (cause instanceof IOException) {
                throw new RewriteException(ErrorCode.AI_UNAVAILABLE);
            }
            throw new IllegalStateException("Gemini request failed unexpectedly", cause);
        }
    }

    String requestBody(String message, Tone tone) {
        ObjectNode userTurn = json.createObjectNode();
        userTurn.put("tone", tone.wireValue());
        userTurn.put("message", message);

        ObjectNode root = json.createObjectNode();
        root.putObject("systemInstruction").putArray("parts").addObject().put("text", systemPrompt);

        ObjectNode content = root.putArray("contents").addObject();
        content.put("role", "user");
        content.putArray("parts").addObject().put("text", json.writeValueAsString(userTurn));

        ObjectNode config = root.putObject("generationConfig");
        config.put("candidateCount", 1);
        config.put("maxOutputTokens", 4096);
        config.put("responseMimeType", "application/json");
        ObjectNode schema = config.putObject("responseJsonSchema");
        schema.put("type", "object");
        ObjectNode rewrites = schema.putObject("properties").putObject("rewrites");
        rewrites.put("type", "array");
        rewrites.putObject("items").put("type", "string");
        rewrites.put("minItems", RewriteLimits.RESULT_COUNT);
        rewrites.put("maxItems", RewriteLimits.RESULT_COUNT);
        schema.putArray("required").add("rewrites");
        schema.put("additionalProperties", false);
        config.putObject("thinkingConfig").put("thinkingLevel", "MINIMAL");

        return json.writeValueAsString(root);
    }

    List<String> parseResponse(String body) {
        JsonNode root = readJson(body);
        if (!root.isObject()) {
            throw invalid();
        }

        JsonNode feedback = root.get("promptFeedback");
        if (feedback != null && feedback.isObject()) {
            JsonNode blockReason = feedback.get("blockReason");
            if (blockReason != null && !blockReason.isNull()) {
                throw new RewriteException(ErrorCode.AI_REFUSED);
            }
        }

        JsonNode candidates = root.get("candidates");
        if (candidates == null || !candidates.isArray() || candidates.size() != 1) {
            throw invalid();
        }
        JsonNode candidate = candidates.get(0);
        if (!candidate.isObject()) {
            throw invalid();
        }

        JsonNode finishReasonNode = candidate.get("finishReason");
        String finishReason = finishReasonNode != null && finishReasonNode.isString()
                ? finishReasonNode.stringValue() : null;
        if (finishReason != null && REFUSAL_FINISH_REASONS.contains(finishReason)) {
            throw new RewriteException(ErrorCode.AI_REFUSED);
        }
        if (!"STOP".equals(finishReason)) {
            throw invalid();
        }

        return parseRewrites(candidateText(candidate));
    }

    private static String candidateText(JsonNode candidate) {
        JsonNode content = candidate.get("content");
        JsonNode parts = content == null ? null : content.get("parts");
        if (parts == null || !parts.isArray()) {
            throw invalid();
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            JsonNode part = parts.get(i);
            if (!part.isObject()) {
                throw invalid();
            }
            JsonNode thought = part.get("thought");
            if (thought != null && thought.isBoolean() && thought.booleanValue()) {
                continue;
            }
            JsonNode partText = part.get("text");
            if (partText != null) {
                if (!partText.isString()) {
                    throw invalid();
                }
                text.append(partText.stringValue());
            } else if (part.properties().stream().anyMatch(field -> !PART_METADATA_FIELDS.contains(field.getKey()))) {
                // Function calls, inline data, etc. are not text we asked for.
                throw invalid();
            }
        }
        if (text.toString().isBlank()) {
            throw invalid();
        }
        return text.toString();
    }

    private List<String> parseRewrites(String modelText) {
        JsonNode result = readJson(modelText);
        if (!result.isObject() || result.size() != 1) {
            throw invalid();
        }
        JsonNode rewrites = result.get("rewrites");
        if (rewrites == null || !rewrites.isArray()) {
            throw invalid();
        }
        List<String> values = new ArrayList<>(rewrites.size());
        for (int i = 0; i < rewrites.size(); i++) {
            JsonNode rewrite = rewrites.get(i);
            if (!rewrite.isString()) {
                throw invalid();
            }
            values.add(rewrite.stringValue());
        }
        return List.copyOf(values);
    }

    private JsonNode readJson(String text) {
        try {
            return json.readTree(text);
        } catch (JacksonException e) {
            throw invalid();
        }
    }

    private static RewriteException invalid() {
        return new RewriteException(ErrorCode.AI_INVALID_RESPONSE);
    }

    private static URI endpointFor(URI baseUrl, String model) {
        String base = baseUrl.toString();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return URI.create(base + "/v1beta/models/" + model + ":generateContent");
    }

    private static String loadPrompt() {
        try (InputStream in = GeminiRewriteClient.class.getClassLoader().getResourceAsStream(PROMPT_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource " + PROMPT_RESOURCE);
            }
            return new String(in.readAllBytes(), UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + PROMPT_RESOURCE, e);
        }
    }
}
