package com.sayless.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sayless.contract.AiRewriteClient;
import com.sayless.contract.ErrorCode;
import com.sayless.contract.RewriteException;
import com.sayless.contract.Tone;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class DefaultRewriteServiceTest {

    private static final List<String> VALID_RESULTS = List.of(
            "Could you send an update?",
            "Please let me know how your section is going.",
            "When do you expect to finish your part?");

    private final RecordingClient client = new RecordingClient();
    private final DefaultRewriteService service = new DefaultRewriteService(client);

    @ParameterizedTest
    @CsvSource({
        "professional, PROFESSIONAL",
        "casual, CASUAL",
        "diplomatic, DIPLOMATIC",
        "funny, FUNNY"
    })
    void sendsEachSupportedToneToProviderExactlyOnce(String wireValue, Tone expectedTone) {
        assertEquals(VALID_RESULTS, service.rewrite("Draft", wireValue));
        assertEquals(1, client.calls);
        assertEquals("Draft", client.message);
        assertSame(expectedTone, client.tone);
    }

    @Test
    void stripsOuterUnicodeWhitespaceAndPreservesDraftContent() {
        String draft = "Hello  team,\n\"Please\" finish \\your section <b>today</b>. 😀";
        service.rewrite("\u2003 \t\n" + draft + "\r\n\u2003", "professional");
        assertEquals(draft, client.message);
        assertEquals(1, client.calls);
    }

    @ParameterizedTest(name = "invalid message: {0}")
    @MethodSource("invalidMessages")
    void invalidMessagesNeverReachProvider(String label, String message) {
        assertFailure(ErrorCode.VALIDATION_ERROR, message, "professional");
        assertEquals(0, client.calls);
    }

    static Stream<Arguments> invalidMessages() {
        return Stream.of(
                Arguments.of("null", (String) null),
                Arguments.of("empty", ""),
                Arguments.of("spaces", "   "),
                Arguments.of("tabs and line breaks", "\t\r\n"),
                Arguments.of("Unicode blank", "\u2003\u2002"),
                Arguments.of("2001 ASCII units", "x".repeat(2001)),
                Arguments.of("supplementary boundary exceeded", "😀".repeat(1000) + "x"));
    }

    @ParameterizedTest(name = "valid message boundary: {0}")
    @MethodSource("validMessageBoundaries")
    void acceptsInclusiveNormalizedUtf16MessageLimit(String label, String message, String expected) {
        service.rewrite(message, "casual");
        assertEquals(expected, client.message);
        assertEquals(1, client.calls);
    }

    static Stream<Arguments> validMessageBoundaries() {
        return Stream.of(
                Arguments.of("minimum one unit", "x", "x"),
                Arguments.of("2000 ASCII units", "x".repeat(2000), "x".repeat(2000)),
                Arguments.of("2000 supplementary units", "😀".repeat(1000), "😀".repeat(1000)),
                Arguments.of("limit after stripping", " \t" + "x".repeat(2000) + "\u2003", "x".repeat(2000)));
    }

    @ParameterizedTest(name = "invalid tone: {0}")
    @MethodSource("invalidTones")
    void invalidTonesNeverReachProvider(String label, String tone) {
        assertFailure(ErrorCode.VALIDATION_ERROR, "Valid draft", tone);
        assertEquals(0, client.calls);
    }

    static Stream<Arguments> invalidTones() {
        return Stream.of(
                Arguments.of("null", (String) null),
                Arguments.of("empty", ""),
                Arguments.of("blank", " \t\n"),
                Arguments.of("unsupported", "friendly"),
                Arguments.of("mixed case", "Professional"),
                Arguments.of("uppercase", "FUNNY"),
                Arguments.of("padded", " professional "),
                Arguments.of("Unicode padding", "\u2003casual\u2003"));
    }

    @Test
    void invalidMessageAndToneDoNotInvokeProvider() {
        assertFailure(ErrorCode.VALIDATION_ERROR, " \n", "not-a-tone");
        assertEquals(0, client.calls);
    }

    @ParameterizedTest(name = "invalid results: {0}")
    @MethodSource("invalidResults")
    void rejectsInvalidResultListsWithoutAnotherProviderAttempt(String label, List<String> results) {
        client.results = results;
        assertFailure(ErrorCode.AI_INVALID_RESPONSE, "Valid draft", "professional");
        assertEquals(1, client.calls);
    }

    static Stream<Arguments> invalidResults() {
        return Stream.of(
                Arguments.of("null list", (List<String>) null),
                Arguments.of("zero options", List.of()),
                Arguments.of("two options", List.of("One", "Two")),
                Arguments.of("four options", List.of("One", "Two", "Three", "Four")),
                Arguments.of("null first", Arrays.asList(null, "Two", "Three")),
                Arguments.of("null middle", Arrays.asList("One", null, "Three")),
                Arguments.of("null last", Arrays.asList("One", "Two", null)),
                Arguments.of("empty string", List.of("", "Two", "Three")),
                Arguments.of("ASCII blank", List.of("One", " \t\r\n", "Three")),
                Arguments.of("Unicode blank", List.of("One", "Two", "\u2003\u2002")),
                Arguments.of("4001-unit option", List.of("x".repeat(4001), "Two", "Three")),
                Arguments.of("supplementary option exceeds limit", List.of("One", "😀".repeat(2000) + "x", "Three")),
                Arguments.of("exact duplicate", List.of("Same", "Same", "Other")),
                Arguments.of("case-only duplicate", List.of("Same", "Other", "SAME")),
                Arguments.of("outer whitespace duplicate", List.of("Same", " \tSame\n", "Other")),
                Arguments.of("interior whitespace duplicate", List.of("Please send it", "PLEASE\n\tSEND  IT", "Other")),
                Arguments.of("Unicode whitespace duplicate", List.of("Go\u00a0home", "go home", "Other")));
    }

    @ParameterizedTest(name = "valid result boundary: {0}")
    @MethodSource("validResultBoundaries")
    void acceptsInclusiveNormalizedUtf16OutputLimit(String label, String raw, String normalized) {
        client.results = List.of(raw, "A different option", "Another different option");
        assertEquals(List.of(normalized, "A different option", "Another different option"),
                service.rewrite("Draft", "professional"));
        assertEquals(1, client.calls);
    }

    static Stream<Arguments> validResultBoundaries() {
        return Stream.of(
                Arguments.of("4000 ASCII units", "x".repeat(4000), "x".repeat(4000)),
                Arguments.of("4000 supplementary units", "😀".repeat(2000), "😀".repeat(2000)),
                Arguments.of("limit after stripping", "\u2003" + "x".repeat(4000) + "\n", "x".repeat(4000)));
    }

    @Test
    void preservesPresentationTextAndOptionOrderWhileStrippingOuterWhitespace() {
        client.results = List.of(
                " \t<b>Hello</b>  team!\nKeep this line.\n",
                "\u2003Second: \"quoted\" \\text 😀\u2003",
                "Third alternative has  two spaces.");
        assertEquals(List.of(
                "<b>Hello</b>  team!\nKeep this line.",
                "Second: \"quoted\" \\text 😀",
                "Third alternative has  two spaces."), service.rewrite("Draft", "funny"));
    }

    @Test
    @ResourceLock("java.util.Locale.default")
    void duplicateComparisonUsesRootLocaleEvenWithTurkishDefault() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            client.results = List.of("I NEED HELP", "i need help", "Other");
            assertFailure(ErrorCode.AI_INVALID_RESPONSE, "Draft", "professional");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void returnsImmutableCopyIndependentOfProviderList() {
        List<String> source = new ArrayList<>(VALID_RESULTS);
        client.results = source;
        List<String> returned = service.rewrite("Draft", "casual");
        assertNotSame(source, returned);
        source.set(0, "Changed after return");
        source.clear();
        assertEquals(VALID_RESULTS, returned);
        assertThrows(UnsupportedOperationException.class, () -> returned.set(0, "Changed"));
        assertThrows(UnsupportedOperationException.class, () -> returned.add("Fourth"));
    }

    @Test
    void consecutiveRequestsKeepIndependentDraftsTonesAndResults() {
        List<String> first = service.rewrite("First draft", "professional");
        client.results = List.of("A new first option", "A new second option", "A new third option");
        List<String> second = service.rewrite("Second draft", "funny");
        assertEquals(VALID_RESULTS, first);
        assertEquals(client.results, second);
        assertEquals("Second draft", client.message);
        assertSame(Tone.FUNNY, client.tone);
        assertEquals(2, client.calls);
    }

    @Test
    void concurrentRequestsKeepTheirOwnArgumentsAndResults() throws Exception {
        CyclicBarrier bothRequests = new CyclicBarrier(2);
        AiRewriteClient overlappingClient = (message, tone) -> {
            try {
                bothRequests.await(5, TimeUnit.SECONDS);
            } catch (Exception error) {
                throw new AssertionError("Requests did not overlap", error);
            }
            return List.of(message + " first " + tone, message + " second " + tone, message + " third " + tone);
        };
        DefaultRewriteService concurrentService = new DefaultRewriteService(overlappingClient);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<List<String>> first = executor.submit(() -> concurrentService.rewrite("Alpha", "casual"));
            Future<List<String>> second = executor.submit(() -> concurrentService.rewrite("Beta", "diplomatic"));
            assertEquals(List.of("Alpha first CASUAL", "Alpha second CASUAL", "Alpha third CASUAL"),
                    first.get(10, TimeUnit.SECONDS));
            assertEquals(List.of("Beta first DIPLOMATIC", "Beta second DIPLOMATIC", "Beta third DIPLOMATIC"),
                    second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @ParameterizedTest
    @MethodSource("providerErrors")
    void propagatesIntentionalProviderExceptionUnchanged(ErrorCode code) {
        RewriteException expected = new RewriteException(code);
        client.failure = expected;
        RewriteException actual = assertThrows(RewriteException.class,
                () -> service.rewrite("Draft", "professional"));
        assertSame(expected, actual);
        assertSame(code, actual.code());
        assertEquals(1, client.calls);
    }

    static Stream<ErrorCode> providerErrors() {
        return Stream.of(ErrorCode.AI_TIMEOUT, ErrorCode.AI_RATE_LIMITED, ErrorCode.AI_REFUSED,
                ErrorCode.AI_UNAVAILABLE, ErrorCode.AI_NOT_CONFIGURED, ErrorCode.AI_INVALID_RESPONSE);
    }

    @Test
    void unexpectedProviderBugIsNotRelabeledAsInvalidOutput() {
        IllegalStateException expected = new IllegalStateException("Synthetic bug");
        client.failure = expected;
        assertSame(expected, assertThrows(IllegalStateException.class,
                () -> service.rewrite("Draft", "professional")));
        assertEquals(1, client.calls);
    }

    @Test
    void canRecoverAfterProviderFailureWithoutRetainingFailedRequest() {
        client.failure = new RewriteException(ErrorCode.AI_UNAVAILABLE);
        assertFailure(ErrorCode.AI_UNAVAILABLE, "First draft", "casual");
        client.failure = null;
        assertEquals(VALID_RESULTS, service.rewrite("New draft", "diplomatic"));
        assertEquals("New draft", client.message);
        assertSame(Tone.DIPLOMATIC, client.tone);
        assertEquals(2, client.calls);
    }

    private void assertFailure(ErrorCode expected, String message, String tone) {
        RewriteException error = assertThrows(RewriteException.class, () -> service.rewrite(message, tone));
        assertSame(expected, error.code());
    }

    private static final class RecordingClient implements AiRewriteClient {
        private List<String> results = VALID_RESULTS;
        private RuntimeException failure;
        private int calls;
        private String message;
        private Tone tone;

        @Override
        public List<String> generate(String message, Tone tone) {
            calls++;
            this.message = message;
            this.tone = tone;
            if (failure != null) {
                throw failure;
            }
            return results;
        }
    }
}
