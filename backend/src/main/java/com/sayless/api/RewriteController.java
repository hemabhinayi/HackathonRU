package com.sayless.api;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.sayless.contract.RewriteRequest;
import com.sayless.contract.RewriteResponse;
import com.sayless.contract.RewriteService;

@RestController
public class RewriteController {

    private final RewriteService rewriteService;

    public RewriteController(RewriteService rewriteService) {
        this.rewriteService = rewriteService;
    }

    // No "consumes" on purpose: a wrong content type is rejected after CORS headers are applied,
    // so the browser can still read the 415 error from an allowed origin.
    @PostMapping(path = "/api/rewrite", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RewriteResponse> rewrite(@RequestBody RewriteRequest request) {
        RewriteResponse response = new RewriteResponse(rewriteService.rewrite(request.message(), request.tone()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }
}
