package com.github.billygk.ai.image.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.billygk.ai.image.config.GeminiProperties
import com.github.billygk.ai.image.dto.GeminiDtos
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.util.Base64

@Service
class GeminiService(
    private val geminiRestClient: RestClient,
    private val geminiProperties: GeminiProperties,
    private val objectMapper: ObjectMapper // Autowired by Spring, useful for debugging JSON
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun describeImage(imageFile: MultipartFile, promptText: String) : String {
        val mimeType = imageFile.contentType
        if (mimeType == null || mimeType !in listOf("image/png", "image/jpeg", "image/webp", "image/heic", "image/heif")) {
            logger.warn("Unsupported image type received: {}", mimeType)
            return "Unsupported image type: $mimeType. Please upload PNG, JPEG, WEBP, HEIC, or HEIF."
        }

        logger.debug("Processing image with MIME type: {} and prompt: '{}'", mimeType, promptText)

        // Prepare image data for Gemini API
        val imageBytes = try {
            imageFile.bytes
        } catch (e: IOException) {
            logger.error("Error reading image bytes from MultipartFile", e)
            throw IOException("Failed to read image file: ${e.message}", e)
        }
        val base64ImageData = Base64.getEncoder().encodeToString(imageBytes)

        val inlineData = GeminiDtos.InlineDataDto(mimeType = mimeType, data = base64ImageData)
        val imagePart  = GeminiDtos.PartDto(inlineData = inlineData)
        val textPart   = GeminiDtos.PartDto(text = promptText)
        val content    = GeminiDtos.ContentDto(parts = listOf(textPart, imagePart))

        val generationConfig = GeminiDtos.GenerationConfigDto(
            temperature     = 0.4f,
            topK            = 32,
            topP            = 1.0f,
            maxOutputTokens = 2048
        )
        val safetySettings = listOf(
            GeminiDtos.SafetySettingDto("HARM_CATEGORY_HARASSMENT", "BLOCK_ONLY_HIGH"),
            GeminiDtos.SafetySettingDto("HARM_CATEGORY_HATE_SPEECH", "BLOCK_ONLY_HIGH"),
            GeminiDtos.SafetySettingDto("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_MEDIUM_AND_ABOVE"),
            GeminiDtos.SafetySettingDto("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_MEDIUM_AND_ABOVE")
        )
        val geminiRequest = GeminiDtos.GeminiRequest(
            contents = listOf(content),
            generationConfig = generationConfig,
            safetySettings = safetySettings
        )

        // Log the request object for debugging (uses Jackson via data class toString or objectMapper)
        logger.debug("Gemini API Request DTO: {}", geminiRequest)
        // For more detailed JSON:
        logger.debug("Gemini API Request JSON: {}", objectMapper.writeValueAsString(geminiRequest))

        try {
            logger.info("Sending request to Gemini API endpoint: {}{}", geminiProperties.baseUrl, geminiProperties.path)

            val responseEntity: ResponseEntity<GeminiDtos.GeminiResponse> = geminiRestClient.post()
                .uri("${geminiProperties.path}?key={apiKey}", geminiProperties.key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(geminiRequest)
                .retrieve()
                .toEntity(GeminiDtos.GeminiResponse::class.java) // Use ::class.java for KClass

            val responseBody = responseEntity.body
            if (responseBody == null) {
                logger.error("Null response body from Gemini API. HTTP Status: {}", responseEntity.statusCode)
                throw RuntimeException("No response body received from Gemini API.")
            }

            logger.debug("Gemini API Full Response DTO: {}", responseBody)

            responseBody.promptFeedback?.takeIf { it.blockReason != null }?.let {
                val blockReason = it.blockReason ?: "Unknown"
                val blockMessage = it.blockReasonMessage ?: "Content was blocked."
                logger.warn("Content generation blocked by Gemini. Reason: {}, Message: {}", blockReason, blockMessage)
                return "Description generation failed. $blockMessage (Reason: $blockReason)"
            }

            val firstCandidate = responseBody.candidates?.firstOrNull()
            if (firstCandidate == null) {
                logger.warn("Gemini API returned no candidates.")
                return "Could not get a description from Gemini. No candidates returned."
            }

            val description = firstCandidate.content?.parts
                ?.mapNotNull { it.text }
                ?.joinToString(" ")
                ?.takeIf { it.isNotBlank() }

            if (description == null) {
                if ("SAFETY".equals(firstCandidate.finishReason, ignoreCase = true)) {
                    logger.warn("Candidate finished due to SAFETY. Safety ratings: {}", firstCandidate.safetyRatings)
                    return "Description generation stopped due to safety concerns."
                }
                logger.warn("Gemini API returned a candidate with no text content.")
                return "Could not get a description from Gemini. No text found in the response."
            }

            logger.info("Successfully received and parsed description from Gemini API.")
            return description

        } catch (e: HttpClientErrorException) {
            logger.error("Client Error during Gemini API call: {} - Response Body: {}", e.statusCode, e.responseBodyAsString, e)
            throw RuntimeException("Error from Gemini API (${e.statusCode}): ${e.responseBodyAsString}", e)
        } catch (e: HttpServerErrorException) {
            logger.error("Server Error during Gemini API call: {} - Response Body: {}", e.statusCode, e.responseBodyAsString, e)
            throw RuntimeException("Gemini API Server Error (${e.statusCode}): ${e.responseBodyAsString}", e)
        } catch (e: Exception) {
            logger.error("Error during Gemini API call with RestClient: {}", e.message, e)
            throw RuntimeException("Error communicating with Gemini API: ${e.message}", e)
        }
    }
}