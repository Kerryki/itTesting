package com.example.testgen.mojo

import com.example.testgen.config.ConfigLoader
import com.example.testgen.domain.GeneratedFile
import com.example.testgen.generator.TestFileGenerator
import com.example.testgen.parser.KotlinCodeParser
import com.example.testgen.scanner.UseCaseScanner
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES)
class GenerateTestsMojo : AbstractMojo() {

    @Parameter(defaultValue = "\${project.basedir}", required = true)
    lateinit var projectBaseDir: File

    @Parameter(defaultValue = "\${project.build.directory}", required = true)
    lateinit var outputBaseDir: File

    @Parameter
    var skipGeneration: Boolean = false

    override fun execute() {
        if (skipGeneration) {
            log.info("Skipping test generation")
            return
        }

        try {
            val config = ConfigLoader.load(projectBaseDir)
            val sourceDir = File(projectBaseDir, config.sourceDir)
            val outputDir = resolveOutputDir(config.outputDir)

            val scanResults = UseCaseScanner(KotlinCodeParser()).scan(sourceDir, config)
            val generator = TestFileGenerator()

            val generatedFiles = scanResults.map { scanResult ->
                // resolvedDtos available via scanResult.resolvedDtos; generator API extended in a future phase
                generator.generateTest(scanResult.useCase)
            }

            generatedFiles.forEach { generatedFile ->
                writeGeneratedFile(generatedFile, outputDir)
            }

            log.info("Generated ${generatedFiles.size} test file(s) into ${outputDir.absolutePath}")
        } catch (e: MojoExecutionException) {
            throw e
        } catch (e: Exception) {
            throw MojoExecutionException("Test scaffolding generation failed: ${e.message}", e)
        }
    }

    private fun resolveOutputDir(configuredOutputDir: String): File {
        val candidate = File(configuredOutputDir)
        return if (candidate.isAbsolute) candidate else File(projectBaseDir, configuredOutputDir)
    }

    private fun writeGeneratedFile(file: GeneratedFile, outputDir: File) {
        val target = File(outputDir, file.filename)
        target.parentFile?.mkdirs()
        target.writeText(file.content)
        log.debug("Wrote ${target.absolutePath}")
    }
}
