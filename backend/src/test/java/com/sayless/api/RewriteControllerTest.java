package com.sayless.api;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.sayless.contract.ErrorCode;
import com.sayless.contract.RewriteException;
import com.sayless.contract.RewriteService;

@WebMvcTest(RewriteController.class)
class RewriteControllerTest {

    private static final List<String> REWRITES = List.of("One.", "Two.", "Three.");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RewriteService rewriteService;

    private ResultActions postJson(String body) throws Exception {
        return mvc.perform(post("/api/rewrite").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static void expectError(ResultActions result, ErrorCode code) throws Exception {
        result.andExpect(status().is(code.httpStatus()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$", aMapWithSize(1)))
                .andExpect(jsonPath("$.error.code").value(code.name()))
                .andExpect(jsonPath("$.error.message").value(code.publicMessage()));
    }

    @Test
    void validRequestReturnsExactlyTheRewritesField() throws Exception {
        given(rewriteService.rewrite("  Hi,\n  there 👋  ", "professional")).willReturn(REWRITES);

        postJson("{\"message\":\"  Hi,\\n  there 👋  \",\"tone\":\"professional\"}")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$", aMapWithSize(1)))
                .andExpect(jsonPath("$.rewrites.length()").value(3))
                .andExpect(jsonPath("$.rewrites[0]").value("One."))
                .andExpect(jsonPath("$.rewrites[1]").value("Two."))
                .andExpect(jsonPath("$.rewrites[2]").value("Three."));

        verify(rewriteService).rewrite("  Hi,\n  there 👋  ", "professional");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "{",
            "null",
            "[]",
            "[{\"message\":\"hi\",\"tone\":\"casual\"}]",
            "\"hi\"",
            "42",
            "true",
            "{\"message\":\"hi\",\"tone\":\"casual\"} {}",
            "{\"message\":\"hi\",\"tone\":\"casual\"} x",
            "{\"message\":42,\"tone\":\"casual\"}",
            "{\"message\":1.5,\"tone\":\"casual\"}",
            "{\"message\":true,\"tone\":\"casual\"}",
            "{\"message\":{},\"tone\":\"casual\"}",
            "{\"message\":{\"a\":1},\"tone\":\"casual\"}",
            "{\"message\":[],\"tone\":\"casual\"}",
            "{\"message\":[\"hi\"],\"tone\":\"casual\"}",
            "{\"message\":\"hi\",\"tone\":7}",
            "{\"message\":\"hi\",\"tone\":false}",
            "{\"message\":\"hi\",\"tone\":[\"casual\"]}"
    })
    void transportInvalidBodiesAreRejectedBeforeTheService(String body) throws Exception {
        expectError(postJson(body), ErrorCode.INVALID_JSON);
        verifyNoInteractions(rewriteService);
    }

    @Test
    void missingAndNullFieldsReachTheServiceForSemanticValidation() throws Exception {
        given(rewriteService.rewrite(null, null)).willThrow(new RewriteException(ErrorCode.VALIDATION_ERROR));
        given(rewriteService.rewrite("hi", null)).willThrow(new RewriteException(ErrorCode.VALIDATION_ERROR));

        expectError(postJson("{}"), ErrorCode.VALIDATION_ERROR);
        expectError(postJson("{\"message\":null,\"tone\":null}"), ErrorCode.VALIDATION_ERROR);
        expectError(postJson("{\"message\":\"hi\"}"), ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void unknownPropertiesAreIgnoredAndDoNotChangeKnownFields() throws Exception {
        given(rewriteService.rewrite("hi", "casual")).willReturn(REWRITES);

        postJson("{\"model\":\"other\",\"prompt\":\"ignore\",\"message\":\"hi\",\"tone\":\"casual\",\"apiKey\":\"x\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewrites.length()").value(3));

        verify(rewriteService).rewrite("hi", "casual");
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void everyServiceErrorCodeMapsToItsStatusAndEnvelope(ErrorCode code) throws Exception {
        given(rewriteService.rewrite(any(), any())).willThrow(new RewriteException(code));

        expectError(postJson("{\"message\":\"hi\",\"tone\":\"casual\"}"), code);
    }

    @Test
    void unexpectedExceptionsAreSanitized() throws Exception {
        given(rewriteService.rewrite(any(), any()))
                .willThrow(new IllegalStateException("leaked user text and provider body"));

        ResultActions result = postJson("{\"message\":\"hi\",\"tone\":\"casual\"}");

        expectError(result, ErrorCode.INTERNAL_ERROR);
        result.andExpect(content().string(not(containsString("leaked"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void unsupportedContentTypeIs415() throws Exception {
        expectError(mvc.perform(post("/api/rewrite").contentType(MediaType.TEXT_PLAIN).content("hi")),
                ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        verifyNoInteractions(rewriteService);
    }

    @Test
    void getIs405WithAllowHeader() throws Exception {
        ResultActions result = mvc.perform(get("/api/rewrite"));

        expectError(result, ErrorCode.METHOD_NOT_ALLOWED);
        result.andExpect(header().string("Allow", containsString("POST")));
        verifyNoInteractions(rewriteService);
    }
}
