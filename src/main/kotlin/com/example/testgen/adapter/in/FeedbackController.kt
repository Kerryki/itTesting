package com.example.testgen.adapter.`in`

import com.example.testgen.domain.ApiResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class FeedbackRequest(
    val filename: String,
    val useful: Boolean,
    val comment: String = ""
)

data class FeedbackStats(
    val totalGenerated: Int,
    val usefulCount: Int
)

@RestController
@RequestMapping("/api")
class FeedbackController(
    @Value("\${app.feedback.log-path:logs/feedback.jsonl}") private val feedbackFilePath: String
) {
    private val maxFileSize = 104857600L // 100MB
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()
    private val rateLimitMap = ConcurrentHashMap<String, AtomicLong>()
    private val rateLimitWindow = 60_000L // 1 minute
    private val maxRequestsPerMinute = 10

    init {
        val dir = File(feedbackFilePath).parentFile
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    @PostMapping("/feedback")
    fun submitFeedback(@RequestBody request: FeedbackRequest): ResponseEntity<ApiResponse<Unit>> {
        return try {
            // Validate fields
            if (!isValidFilename(request.filename)) {
                return ResponseEntity(
                    ApiResponse.error<Unit>("Invalid filename format"),
                    HttpStatus.BAD_REQUEST
                )
            }

            if (request.comment.length > 500) {
                return ResponseEntity(
                    ApiResponse.error<Unit>("Comment exceeds maximum length of 500 characters"),
                    HttpStatus.BAD_REQUEST
                )
            }

            // Check file size before writing
            val path = Paths.get(feedbackFilePath)
            if (Files.exists(path) && Files.size(path) > maxFileSize) {
                logger.error("Feedback log file exceeded maximum size of $maxFileSize bytes")
                return ResponseEntity(
                    ApiResponse.error<Unit>("Feedback log is full"),
                    HttpStatus.INTERNAL_SERVER_ERROR
                )
            }

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            val sanitizedComment = request.comment
                .replace("\n", " ")
                .replace("\r", " ")
                .take(500)

            val feedbackEntry = mapOf(
                "timestamp" to timestamp,
                "filename" to request.filename,
                "useful" to request.useful,
                "comment" to sanitizedComment
            )

            val jsonLine = objectMapper.writeValueAsString(feedbackEntry) + "\n"

            Files.write(
                path,
                jsonLine.toByteArray(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            )

            ResponseEntity.ok(ApiResponse.success(Unit))
        } catch (e: Exception) {
            logger.error("Error saving feedback", e)
            ResponseEntity(
                ApiResponse.error<Unit>("An unexpected error occurred while saving feedback"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @GetMapping("/stats")
    fun getStats(): ResponseEntity<ApiResponse<FeedbackStats>> {
        return try {
            val path = Paths.get(feedbackFilePath)
            val stats = if (Files.exists(path)) {
                var totalGenerated = 0
                var usefulCount = 0

                // Stream file to avoid loading entire file into memory
                Files.lines(path).use { stream ->
                    totalGenerated = stream.count().toInt()
                }

                // Second pass to count useful entries
                Files.lines(path).use { stream ->
                    usefulCount = stream
                        .filter { line: String ->
                            runCatching {
                                val jsonNode = objectMapper.readTree(line)
                                jsonNode?.get("useful")?.asBoolean() == true
                            }.getOrDefault(false)
                        }
                        .count()
                        .toInt()
                }

                FeedbackStats(totalGenerated, usefulCount)
            } else {
                FeedbackStats(0, 0)
            }

            ResponseEntity.ok(ApiResponse.success(stats))
        } catch (e: Exception) {
            logger.error("Error reading stats", e)
            ResponseEntity(
                ApiResponse.error<FeedbackStats>("An unexpected error occurred while reading stats"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    private fun isValidFilename(filename: String): Boolean {
        // Allow alphanumeric, dots, hyphens, underscores
        return filename.matches(Regex("^[a-zA-Z0-9._-]+\\.kt$")) && filename.length <= 255
    }
}
