package com.sayless.service;

import com.sayless.contract.AiRewriteClient;
import com.sayless.contract.ErrorCode;
import com.sayless.contract.RewriteException;
import com.sayless.contract.RewriteLimits;
import com.sayless.contract.RewriteService;
import com.sayless.contract.Tone;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Validates drafts and generated options without retaining request content. */
@Service
public final class DefaultRewriteService implements RewriteService {

    private static final Pattern WHITESPACE = Pattern.compile("(?U)\\s+");

    private final AiRewriteClient client;

    public DefaultRewriteService(AiRewriteClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public List<String> rewrite(String message, String tone) {
        if (message == null) {
            throw new RewriteException(ErrorCode.VALIDATION_ERROR);
        }
        String normalizedMessage = message.strip();
        if (normalizedMessage.isBlank()
                || normalizedMessage.length() > RewriteLimits.MAX_MESSAGE_LENGTH) {
            throw new RewriteException(ErrorCode.VALIDATION_ERROR);
        }
        Tone selectedTone = Tone.fromWireValue(tone);
        return validateResults(client.generate(normalizedMessage, selectedTone));
    }

    private static List<String> validateResults(List<String> rewrites) {
        if (rewrites == null || rewrites.size() != RewriteLimits.RESULT_COUNT) {
            throw new RewriteException(ErrorCode.AI_INVALID_RESPONSE);
        }
        List<String> normalizedResults = new ArrayList<>(RewriteLimits.RESULT_COUNT);
        Set<String> comparisonKeys = new HashSet<>();
        for (String rewrite : rewrites) {
            if (rewrite == null) {
                throw new RewriteException(ErrorCode.AI_INVALID_RESPONSE);
            }
            String normalized = rewrite.strip();
            if (normalized.isBlank() || normalized.length() > RewriteLimits.MAX_REWRITE_LENGTH) {
                throw new RewriteException(ErrorCode.AI_INVALID_RESPONSE);
            }
            String key = WHITESPACE.matcher(normalized).replaceAll(" ").toLowerCase(Locale.ROOT);
            if (!comparisonKeys.add(key)) {
                throw new RewriteException(ErrorCode.AI_INVALID_RESPONSE);
            }
            normalizedResults.add(normalized);
        }
        return List.copyOf(normalizedResults);
    }
}
