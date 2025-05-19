package com.github.billygk.ai.image.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

object GeminiDtos {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class GeminiRequest(
        val contents: List<ContentDto>,
        @JsonProperty("generation_config")
        val generationConfig: GenerationConfigDto? = null, // Made nullable for flexibility
        @JsonProperty("safety_settings")
        val safetySettings: List<SafetySettingDto>? = null // Made nullable for flexibility
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class ContentDto(
        val parts: List<PartDto>,
        val role: String? = "user" // Default role to "user"
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class PartDto(
        val text: String? = null,
        @JsonProperty("inline_data")
        val inlineData: InlineDataDto? = null
    )

    data class InlineDataDto(
        @JsonProperty("mime_type")
        val mimeType: String,
        val data: String // Base64 encoded string
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class GenerationConfigDto(
        val temperature: Float? = null,
        val topK: Int? = null,
        val topP: Float? = null,
        @JsonProperty("max_output_tokens")
        val maxOutputTokens: Int? = null
        // val stopSequences: List<String>? = null
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class SafetySettingDto(
        val category: String, // e.g., "HARM_CATEGORY_HARASSMENT"
        val threshold: String // e.g., "BLOCK_MEDIUM_AND_ABOVE"
    )

    // Response DTOs
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class GeminiResponse(
        val candidates: List<CandidateDto>? = null,
        @JsonProperty("prompt_feedback")
        val promptFeedback: PromptFeedbackDto? = null
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class CandidateDto(
        val content: ContentDto?,
        @JsonProperty("finish_reason")
        val finishReason: String? = null,
        val index: Int? = null,
        @JsonProperty("safety_ratings")
        val safetyRatings: List<SafetyRatingDto>? = null,
        @JsonProperty("token_count")
        val tokenCount: Int? = null
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class SafetyRatingDto(
        val category: String,
        val probability: String, // e.g., "NEGLIGIBLE", "LOW", "MEDIUM", "HIGH"
        val blocked: Boolean? = null
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class PromptFeedbackDto(
        @JsonProperty("block_reason")
        val blockReason: String? = null,
        @JsonProperty("safety_ratings")
        val safetyRatings: List<SafetyRatingDto>? = null,
        @JsonProperty("block_reason_message")
        val blockReasonMessage: String? = null
    )

}