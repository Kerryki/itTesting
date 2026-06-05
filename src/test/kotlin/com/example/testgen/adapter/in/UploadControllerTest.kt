package com.example.testgen.adapter.`in`

import com.example.testgen.domain.ApiResponse
import com.example.testgen.domain.GeneratedFile
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.file.Files
import java.nio.file.Paths

@SpringBootTest
@AutoConfigureMockMvc
class UploadControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `upload should return 200 with generated test`() {
        val fileContent = Files.readString(Paths.get("src/test/resources/dummy/SimpleUseCase.kt"))

        mockMvc.perform(
            post("/api/upload")
                .contentType(MediaType.TEXT_PLAIN)
                .content(fileContent)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.filename").exists())
            .andExpect(jsonPath("$.data.content").exists())
    }

    @Test
    fun `upload should return generated test with correct class name`() {
        val fileContent = Files.readString(Paths.get("src/test/resources/dummy/SimpleUseCase.kt"))

        mockMvc.perform(
            post("/api/upload")
                .contentType(MediaType.TEXT_PLAIN)
                .content(fileContent)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.filename").value("SimpleUseCaseTest.kt"))
    }

    @Test
    fun `upload should return error for invalid file`() {
        val invalidContent = "this is not a valid kotlin file"

        mockMvc.perform(
            post("/api/upload")
                .contentType(MediaType.TEXT_PLAIN)
                .content(invalidContent)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").exists())
    }
}
