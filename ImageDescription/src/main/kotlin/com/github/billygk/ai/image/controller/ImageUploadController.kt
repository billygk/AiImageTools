package com.github.billygk.ai.image.controller

import com.github.billygk.ai.image.service.GeminiService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@Controller
@RequestMapping("/api/v1/image") // Using a base path for API versioning
class ImageUploadController(private val geminiService: GeminiService) { // Constructor injection

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/upload")
    fun handleImageUpload(
        @RequestParam("imageFile") file: MultipartFile,
        @RequestParam(value = "prompt", defaultValue = "Describe this image in detail.") prompt: String
    ): ResponseEntity<Map<String, String>> {
        if (file.isEmpty) {
            logger.warn("Upload attempt with no file selected.")
            return ResponseEntity.badRequest().body(mapOf("error" to "No file selected for upload."))
        }

        if (file.size > 4 * 1024 * 1024) { // 4MB limit for inline data
            logger.warn("Upload attempt with file exceeding 4MB: {} bytes", file.size)
            return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(mapOf("error" to "File size exceeds the 4MB limit."))
        }

        return try {
            logger.info("Handling image upload. File: ${file.originalFilename}, Size: ${file.size}, Prompt: '$prompt'")
            val description = geminiService.describeImage(file, prompt)
            logger.info("Successfully processed image upload for file: ${file.originalFilename}")
            ResponseEntity.ok(mapOf("description" to description))
        } catch (e: IllegalArgumentException) { // Catch specific exceptions from service for bad requests
            logger.warn("API: Bad request during image upload: ${e.message}", e)
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request parameters.")))
        }
        catch (e: IOException) { // For issues reading the file
            logger.error("API: IOException during image upload for file: ${file.originalFilename}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to read or process image file: ${e.message}"))
        } catch (e: RuntimeException) { // Catch runtime exceptions from GeminiService (API errors, etc.)
            logger.error("API: RuntimeException during image upload for file: ${file.originalFilename}", e)
            val causeMessage = e.cause?.message ?: e.message
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR) // Or a more specific error code if possible
                .body(mapOf("error" to "Error processing image with Gemini: $causeMessage"))
        } catch (e: Exception) { // General catch-all
            logger.error("API: An unexpected error occurred during upload for file: ${file.originalFilename}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "An unexpected error occurred: ${e.message}"))
        }
    }
}