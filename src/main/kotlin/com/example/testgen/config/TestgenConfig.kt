package com.example.testgen.config

data class TestgenConfig(
    val sourceDir: String = "src/main/kotlin",
    val outputDir: String = "src/test/kotlin",
    val basePackage: String = "",
    val includes: List<String> = emptyList(),   // glob patterns to include
    val excludes: List<String> = emptyList(),   // glob patterns to exclude
    val templatePath: String? = null            // optional custom template override
)
