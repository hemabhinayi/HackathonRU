package com.sayless.integration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;

import com.sayless.contract.AiRewriteClient;
import com.sayless.provider.GeminiRewriteClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Explicit properties override any ambient SAYLESS_AI_ENABLED / GEMINI_API_KEY on a developer machine. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"sayless.ai.enabled=false", "sayless.ai.api-key="})
class DisabledModeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext context;

    @Test
    void bootsWithoutAKeyAndUsesOnlyTheDisabledClient() {
        assertThat(context.getBeansOfType(AiRewriteClient.class)).hasSize(1);
        assertThat(context.getBeansOfType(GeminiRewriteClient.class)).isEmpty();
    }

    @Test
    void validRewriteReturnsNotConfiguredInsteadOfSampleText() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/rewrite"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"message\":\"I need my teammate to finish their part\",\"tone\":\"professional\"}", UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(UTF_8));

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
        JsonNode body = JsonMapper.builder().build().readTree(response.body());
        assertThat(body.at("/error/code").stringValue()).isEqualTo("AI_NOT_CONFIGURED");
        assertThat(body.has("rewrites")).isFalse();
        assertThat(response.body()).doesNotContain("Could you let me know");
    }
}
