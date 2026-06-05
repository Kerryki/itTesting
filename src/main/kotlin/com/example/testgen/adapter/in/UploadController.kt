package com.example.testgen.adapter.`in`

import com.example.testgen.domain.ApiResponse
import com.example.testgen.domain.GeneratedFile
import com.example.testgen.parser.KotlinCodeParser
import com.example.testgen.generator.TestFileGenerator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class UploadController(
    @Autowired private val parser: KotlinCodeParser,
    @Autowired private val generator: TestFileGenerator
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val maxFileSize = 1_000_000 // 1MB

    @PostMapping("/upload", consumes = [MediaType.TEXT_PLAIN_VALUE])
    fun uploadFile(@RequestBody content: String): ResponseEntity<ApiResponse<GeneratedFile>> {
        return try {
            // Validate request size
            if (content.length > maxFileSize) {
                return ResponseEntity(
                    ApiResponse.error<GeneratedFile>("File exceeds maximum size of 1MB"),
                    HttpStatus.PAYLOAD_TOO_LARGE
                )
            }

            // Validate content is not empty
            if (content.isBlank()) {
                return ResponseEntity(
                    ApiResponse.error<GeneratedFile>("File content cannot be empty"),
                    HttpStatus.BAD_REQUEST
                )
            }

            val useCase = parser.parseUseCase(content)
                ?: return ResponseEntity(
                    ApiResponse.error<GeneratedFile>("Invalid Kotlin file: could not find a use case class (expected class ending with 'UseCase')"),
                    HttpStatus.BAD_REQUEST
                )

            val requestDto = parser.parseDataClass(content, useCase.requestTypeName)
                ?: return ResponseEntity(
                    ApiResponse.error<GeneratedFile>("Could not find request DTO class '${useCase.requestTypeName}' in the file"),
                    HttpStatus.BAD_REQUEST
                )

            val responseDto = parser.parseDataClass(content, useCase.responseTypeName)
                ?: return ResponseEntity(
                    ApiResponse.error<GeneratedFile>("Could not find response DTO class '${useCase.responseTypeName}' in the file"),
                    HttpStatus.BAD_REQUEST
                )

            val useCaseWithFields = useCase.copy(
                requestFields = requestDto.fields,
                responseFields = responseDto.fields
            )

            val generatedFile = generator.generateTest(useCaseWithFields)

            ResponseEntity.ok(ApiResponse.success(generatedFile))
        } catch (e: Exception) {
            logger.error("Error processing uploaded file", e)
            ResponseEntity(
                ApiResponse.error<GeneratedFile>("An unexpected error occurred. Please try again."),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }
}
