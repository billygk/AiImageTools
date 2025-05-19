package com.github.billygk.ai.image.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class RestClientConfig {

    @Bean
    fun geminiRestClient(geminiProperties: GeminiProperties): RestClient {
        val jdkHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)) // Connection timeout
            .build()

        val requestFactory = JdkClientHttpRequestFactory(jdkHttpClient)
        // Set read timeout at factory level
        requestFactory.setReadTimeout(Duration.ofSeconds(60))

        return RestClient.builder()
            .baseUrl(geminiProperties.baseUrl) // Base URL from properties
            .requestFactory(requestFactory) // Using JDK's HTTP client
            .defaultHeader("Content-Type", "application/json")
            .build()
    }

}