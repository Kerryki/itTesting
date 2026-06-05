package com.example.testgen.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule
import java.io.File

object ConfigLoader {

    private val reader: ObjectReader = YAMLMapper.builder()
        .addModule(kotlinModule { configure(KotlinFeature.NullIsSameAsDefault, true) })
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()
        .readerFor(TestgenConfig::class.java)

    fun load(projectBaseDir: File): TestgenConfig {
        require(projectBaseDir.exists()) {
            "projectBaseDir does not exist: ${projectBaseDir.absolutePath}"
        }
        require(projectBaseDir.isDirectory) {
            "projectBaseDir is not a directory: ${projectBaseDir.absolutePath}"
        }

        val configFile = File(projectBaseDir, ".testgen.yml")

        if (!configFile.exists()) {
            return TestgenConfig()
        }

        return try {
            reader.readValue(configFile)
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "Failed to parse .testgen.yml at '${configFile.absolutePath}': ${e.message}",
                e
            )
        }
    }
}
