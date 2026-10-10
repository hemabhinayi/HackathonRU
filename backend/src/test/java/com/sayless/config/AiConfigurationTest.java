package com.sayless.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.context.annotation.UserConfigurations;

import com.sayless.contract.AiRewriteClient;
import com.sayless.contract.ErrorCode;
import com.sayless.contract.RewriteException;
import com.sayless.contract.Tone;

class AiConfigurationTest {

    private static final String SECRET = "test-key-SHOULD-NEVER-APPEAR";

    private static final String[] VALID_ENABLED = {
            "sayless.ai.enabled=true",
            "sayless.ai.api-key=" + SECRET,
            "sayless.ai.model=gemini-3.5-flash-lite",
            "sayless.ai.base-url=https://generativelanguage.googleapis.com",
            "sayless.ai.connect-timeout=3s",
            "sayless.ai.request-timeout=20s"
    };

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(UserConfigurations.of(AiConfiguration.class));

    @Test
    void disabledByDefaultProvidesOneClientThatRefusesWithoutNetwork() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(AiRewriteClient.class);
            AiRewriteClient client = context.getBean(AiRewriteClient.class);
            assertThatThrownBy(() -> client.generate("hello", Tone.CASUAL))
                    .isInstanceOfSatisfying(RewriteException.class,
                            ex -> assertThat(ex.code()).isEqualTo(ErrorCode.AI_NOT_CONFIGURED));
        });
    }

    @Test
    void explicitlyDisabledStartsWithBlankKey() {
        runner.withPropertyValues("sayless.ai.enabled=false", "sayless.ai.api-key=")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(AiRewriteClient.class));
    }

    @Test
    void enabledWithValidConfigBindsAndLeavesClientToTheGeminiProvider() {
        runner.withPropertyValues(VALID_ENABLED).run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(AiRewriteClient.class);
            AiProperties properties = context.getBean(AiProperties.class);
            assertThat(properties.model()).isEqualTo("gemini-3.5-flash-lite");
            assertThat(properties.baseUrl()).isEqualTo(URI.create("https://generativelanguage.googleapis.com"));
            assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
            assertThat(properties.requestTimeout()).isEqualTo(Duration.ofSeconds(20));
        });
    }

    @Test
    void enabledAllowsHttpLoopbackForTests() {
        runner.withPropertyValues(VALID_ENABLED)
                .withPropertyValues("sayless.ai.base-url=http://127.0.0.1:8089")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sayless.ai.api-key=",
            "sayless.ai.api-key=   ",
            "sayless.ai.model=",
            "sayless.ai.model=models/gemini",
            "sayless.ai.model=gemini?key=x",
            "sayless.ai.base-url=http://generativelanguage.googleapis.com",
            "sayless.ai.base-url=ftp://example.com",
            "sayless.ai.base-url=https://example.com/?key=x",
            "sayless.ai.base-url=https://user:pass@example.com",
            "sayless.ai.base-url=not a url",
            "sayless.ai.connect-timeout=0s",
            "sayless.ai.request-timeout=-1s",
            "sayless.ai.connect-timeout=21s",
            "sayless.ai.request-timeout=26s"
    })
    void invalidEnabledConfigFailsStartupWithoutLeakingTheKey(String override) {
        runner.withPropertyValues(VALID_ENABLED).withPropertyValues(override).run(context -> {
            assertThat(context).hasFailed();
            StringWriter trace = new StringWriter();
            context.getStartupFailure().printStackTrace(new PrintWriter(trace));
            assertThat(trace.toString()).contains("sayless.ai").doesNotContain(SECRET);
        });
    }

    @Test
    void toStringRedactsTheKey() {
        AiProperties properties = new AiProperties(true, SECRET, "gemini-3.5-flash-lite",
                URI.create("https://generativelanguage.googleapis.com"), Duration.ofSeconds(3), Duration.ofSeconds(20));

        assertThat(properties.toString()).doesNotContain(SECRET).contains("<redacted>");
    }
}
