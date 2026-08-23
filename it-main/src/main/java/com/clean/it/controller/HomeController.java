package com.clean.it.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

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
