package com.example.testgen.adapter.`in`

import com.example.testgen.domain.ApiResponse
import com.example.testgen.domain.GeneratedFile
import com.example.testgen.parser.KotlinCodeParser
import com.example.testgen.generator.TestFileGenerator
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class UploadController {
    private val parser = KotlinCodeParser()
    private val generator = TestFileGenerator()

    @PostMapping("/upload")
    fun uploadFile(@RequestBody content: String): ResponseEntity<ApiResponse<GeneratedFile>> {
        return try {
            val useCase = parser.parseUseCase(content)
                ?: return ResponseEntity(
                    ApiResponse.error<GeneratedFile>("Failed to parse use case from file"),
                    HttpStatus.BAD_REQUEST
                )

            val requestDto = parser.parseDataClass(content, useCase.requestTypeName)
                ?: return ResponseEntity(
                    ApiResponse.error<GeneratedFile>("Failed to parse request DTO"),
                    HttpStatus.BAD_REQUEST
                )

            val responseDto = parser.parseDataClass(content, useCase.responseTypeName)
                ?: return ResponseEntity(
                    ApiResponse.error<GeneratedFile>("Failed to parse response DTO"),
                    HttpStatus.BAD_REQUEST
                )

            val useCaseWithFields = useCase.copy(
                requestFields = requestDto.fields,
                responseFields = responseDto.fields
            )

            val generatedFile = generator.generateTest(useCaseWithFields)

            ResponseEntity.ok(ApiResponse.success(generatedFile))
        } catch (e: Exception) {
            ResponseEntity(
                ApiResponse.error<GeneratedFile>("Error processing file: ${e.message}"),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }
}
