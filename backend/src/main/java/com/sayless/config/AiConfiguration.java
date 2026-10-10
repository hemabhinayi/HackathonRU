package com.sayless.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sayless.contract.AiRewriteClient;
import com.sayless.contract.ErrorCode;
import com.sayless.contract.RewriteException;

/**
 * When sayless.ai.enabled is false (the default), this class supplies the only AiRewriteClient.
 * When true, Person 3's GeminiRewriteClient must be the single client, guarded by
 * {@code @ConditionalOnBooleanProperty("sayless.ai.enabled")}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiProperties.class)
public class AiConfiguration {

    @Bean
    @ConditionalOnBooleanProperty(name = "sayless.ai.enabled", havingValue = false, matchIfMissing = true)
    AiRewriteClient disabledAiRewriteClient() {
        return (normalizedMessage, tone) -> {
            throw new RewriteException(ErrorCode.AI_NOT_CONFIGURED);
        };
    }
}
