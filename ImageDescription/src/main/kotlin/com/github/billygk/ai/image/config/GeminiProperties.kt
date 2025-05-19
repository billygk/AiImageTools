package com.github.billygk.ai.image.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "gemini.api")
@Validated
data class GeminiProperties (
    @field:NotBlank(message = "Gemini API key must be configured (gemini.api.key)")
    var key: String = "", // Default values needed for no-arg constructor or use @ConstructorBinding

    @field:NotBlank(message = "Gemini API base URL must be configured (gemini.api.base-url)")
    var baseUrl: String = "",

    @field:NotBlank(message = "Gemini API path must be configured (gemini.api.path)")
    var path: String = ""
) {}
