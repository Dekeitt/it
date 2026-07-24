package com.clean.it.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> home() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/index.html");
        String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @GetMapping("/api/info")
    @ResponseBody
    public Map<String, Object> info() {
        return Map.of(
                "name", "it API",
                "status", "ok",
                "docs", Map.of(
                        "swaggerUi", "/swagger-ui/index.html",
                        "openApi", "/v3/api-docs"
                )
        );
    }
}
