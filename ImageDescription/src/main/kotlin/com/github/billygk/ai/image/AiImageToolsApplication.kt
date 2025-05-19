package com.github.billygk.ai.image

import com.github.billygk.ai.image.config.GeminiProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(GeminiProperties::class)
class AiImageToolsApplication

fun main(args: Array<String>) {
	runApplication<AiImageToolsApplication>(*args)
}
