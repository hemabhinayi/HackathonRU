package com.sayless.config;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sayless.api.RewriteController;
import com.sayless.contract.ErrorCode;
import com.sayless.contract.RewriteException;
import com.sayless.contract.RewriteService;

@WebMvcTest(RewriteController.class)
class WebConfigurationTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RewriteService rewriteService;

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5500", "http://127.0.0.1:5500"})
    void preflightFromAllowedOriginsSucceeds(String origin) throws Exception {
        mvc.perform(options("/api/rewrite")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("content-type")))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    void preflightFromDisallowedOriginIsNotAuthorized() throws Exception {
        mvc.perform(options("/api/rewrite")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void actualRequestFromDisallowedOriginIsRejectedWithoutCallingService() throws Exception {
        mvc.perform(post("/api/rewrite")
                        .header("Origin", "http://evil.example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\",\"tone\":\"casual\"}"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        org.mockito.Mockito.verifyNoInteractions(rewriteService);
    }

    @Test
    void errorResponsesRemainReadableFromAllowedOrigin() throws Exception {
        given(rewriteService.rewrite(any(), any())).willThrow(new RewriteException(ErrorCode.AI_UNAVAILABLE));

        mvc.perform(post("/api/rewrite")
                        .header("Origin", "http://localhost:5500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\",\"tone\":\"casual\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5500"))
                .andExpect(jsonPath("$.error.code").value("AI_UNAVAILABLE"));
    }

    @Test
    void invalidJsonErrorRemainsReadableFromAllowedOrigin() throws Exception {
        mvc.perform(post("/api/rewrite")
                        .header("Origin", "http://localhost:5500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5500"))
                .andExpect(jsonPath("$.error.code").value("INVALID_JSON"));
    }

    @Test
    void corsPropertiesRejectWildcardsAndDropBlanks() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CorsProperties(List.of("*")));
        assertThatIllegalArgumentException().isThrownBy(() -> new CorsProperties(List.of("https://*.example.com")));
        assertThat(new CorsProperties(List.of(" http://localhost:5500 ", "")).allowedOrigins())
                .containsExactly("http://localhost:5500");
        assertThat(new CorsProperties(null).allowedOrigins()).isEmpty();
    }
}
