package com.github.billygk.ai.image.controller

import com.github.billygk.ai.image.exceptions.FileSizeLimitExceededException
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

    fun validateImage(file: MultipartFile, maxSizeMB: Int = 4) {
        if (file.isEmpty) {
            throw IllegalArgumentException("No file selected for upload.")
        }

        val maxSizeBytes = maxSizeMB * 1024 * 1024
        if (file.size > maxSizeBytes) {
            val fileSizeMB = String.format("%.2f", file.size.toDouble() / (1024 * 1024))
            throw FileSizeLimitExceededException("File size of ${fileSizeMB}MB exceeds the ${maxSizeMB}MB limit.")
        }

        val allowedMimeTypes = setOf("image/jpeg", "image/png", "image/gif", "image/webp")
        // Ensure contentType is not null and is converted to lowercase for case-insensitive comparison
        val contentType = file.contentType?.lowercase()

        if (contentType == null || !allowedMimeTypes.contains(contentType)) {
            val providedContentType = file.contentType ?: "unknown"
            throw IllegalArgumentException("Invalid file type: '$providedContentType'. Allowed types are JPEG, PNG, GIF, WEBP.")
        }
    }

    @PostMapping("/upload")
    fun handleImageUpload(
        @RequestParam("imageFile") file: MultipartFile,
        @RequestParam(value = "prompt", defaultValue = "Describe this image in detail.") prompt: String
    ): ResponseEntity<Map<String, String>> {

        validateImage(file)

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

    /**
     * Upload image and count calories, filter out any image that is not food related, therefor its calories can not be counted
     */

    /**
     * Upload image and count calories, filter out any image that is not food related, therefor its calories can not be counted
     */
    @PostMapping("/calories-count")
    fun handleCaloriesCount(
        @RequestParam("imageFile") file: MultipartFile
    ): ResponseEntity<Map<String, String>> { // Assuming the response is a map with string values
        try {
            validateImage(file) // Use the same common validation function
            logger.info("Handling calorie count for image. File: ${file.originalFilename ?: "unknown_file"}, Size: ${file.size}")

            val prompt = "Is this image of food? If yes, provide an estimated calorie count and a brief description of the food " +
                    "items. If not, state that it is not food and no calories can be counted." +
                    "Output should be json format like: " +
                    "Calories: 100," +
                    "description: ...,"

            // Assuming geminiService.describeImage can handle this prompt,
            // or a more specific service method like geminiService.getCalorieInfo(file, prompt) would be used.
            val calorieInfo = geminiService.describeImage(file, prompt)

            logger.info("Successfully processed calorie count for file: ${file.originalFilename ?: "unknown_file"}")
            return ResponseEntity.ok(mapOf("response" to calorieInfo))

        } catch (e: FileSizeLimitExceededException) {
            logger.warn("API: File size limit exceeded for calorie count (file: ${file.originalFilename ?: "unknown_file"}). Error: ${e.message}")
            return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(mapOf("error" to (e.message ?: "File size exceeds the limit.")))
        } catch (e: IllegalArgumentException) {
            logger.warn("API: Bad request during calorie count (file: ${file.originalFilename ?: "unknown_file"}). Error: ${e.message}")
            return ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request parameters.")))
        } catch (e: IOException) {
            logger.error("API: IOException during calorie count (file: ${file.originalFilename ?: "unknown_file"})", e)
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to read or process image file: ${e.message}"))
        } catch (e: RuntimeException) { // Catch runtime exceptions from GeminiService (API errors, etc.)
            logger.error(
                "API: RuntimeException during calorie count (file: ${file.originalFilename ?: "unknown_file"})",
                e
            )
            val causeMessage = e.cause?.message ?: e.message
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Error processing image for calorie count with Gemini: $causeMessage"))
        } catch (e: Exception) { // General catch-all
            logger.error(
                "API: An unexpected error occurred during calorie count (file: ${file.originalFilename ?: "unknown_file"})",
                e
            )
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "An unexpected error occurred: ${e.message}"))
        }
    }


}