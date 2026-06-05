package com.example.testgen.adapter.`in`

import com.example.testgen.domain.ApiResponse
import com.fasterxml.jackson.databind.ObjectMapper
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
class FeedbackController {
    private val feedbackFile = "logs/feedback.jsonl"
    private val objectMapper = ObjectMapper()

    init {
        val dir = File("logs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    @PostMapping("/feedback")
    fun submitFeedback(@RequestBody request: FeedbackRequest): ResponseEntity<ApiResponse<Unit>> {
        return try {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            val feedbackEntry = mapOf(
                "timestamp" to timestamp,
                "filename" to request.filename,
                "useful" to request.useful,
                "comment" to request.comment
            )

            val jsonLine = objectMapper.writeValueAsString(feedbackEntry) + "\n"

            Files.write(
                Paths.get(feedbackFile),
                jsonLine.toByteArray(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            )

            ResponseEntity.ok(ApiResponse.success(Unit))
        } catch (e: Exception) {
            ResponseEntity(
                ApiResponse.error<Unit>("Error saving feedback: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    @GetMapping("/stats")
    fun getStats(): ResponseEntity<ApiResponse<FeedbackStats>> {
        return try {
            val path = Paths.get(feedbackFile)
            val stats = if (Files.exists(path)) {
                val lines = Files.readAllLines(path)
                val totalGenerated = lines.size
                val usefulCount = lines.count { line ->
                    line.contains("\"useful\":true")
                }
                FeedbackStats(totalGenerated, usefulCount)
            } else {
                FeedbackStats(0, 0)
            }

            ResponseEntity.ok(ApiResponse.success(stats))
        } catch (e: Exception) {
            ResponseEntity(
                ApiResponse.error<FeedbackStats>("Error reading stats: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }
}
