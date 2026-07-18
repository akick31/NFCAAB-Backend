package com.nfcaab.backend.config

import com.nfcaab.backend.security.JwtAuthenticationFilter
import com.nfcaab.backend.service.auth.SessionService
import com.nfcaab.backend.service.user.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
open class SecurityConfig(
    private val sessionService: SessionService,
    private val userService: UserService,
    @Value("\${bot.api.key}") private val botApiKey: String,
    @Value("\${cookie.domain:}") private val cookieDomain: String,
    @Value("\${cookie.secure:true}") private val cookieSecure: Boolean,
) {
    @Bean
    open fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    open fun corsConfigurationSource(): CorsConfigurationSource {
        val config =
            CorsConfiguration().apply {
                allowedOrigins = listOf("https://nfcaab.com", "https://www.nfcaab.com", "http://localhost:3000")
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf("Content-Type", "Authorization", "X-NFCAAB-Api-Key", "X-XSRF-TOKEN")
                allowCredentials = true
            }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", config) }
    }

    private fun cookieBackedMutationMatcher(): RequestMatcher =
        RequestMatcher { request ->
            val unsafe = request.method !in setOf("GET", "HEAD", "OPTIONS", "TRACE")
            val hasApiKey = request.getHeader("X-NFCAAB-Api-Key") != null
            val hasBearer = request.getHeader("Authorization")?.startsWith("Bearer ") == true
            unsafe && !hasApiKey && !hasBearer
        }

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val jwtAuthenticationFilter = JwtAuthenticationFilter(sessionService, userService, botApiKey)
        http
            .cors().configurationSource(corsConfigurationSource())
            .and()
            .csrf()
            .csrfTokenRepository(
                CookieCsrfTokenRepository.withHttpOnlyFalse().apply {
                    setCookieDomain(cookieDomain)
                    setSecure(cookieSecure)
                },
            )
            .requireCsrfProtectionMatcher(cookieBackedMutationMatcher())
            .ignoringAntMatchers(
                "/auth/register",
                "/auth/login",
                "/auth/forgot-password",
                "/auth/reset-password",
                "/lineup/submit",
                "/internal/frontend-errors",
            )
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .antMatchers("/auth/**", "/health/**", "/internal/frontend-errors", "/discord/**").permitAll()
            .antMatchers(HttpMethod.GET, "/lineup/token/*").permitAll()
            .antMatchers(HttpMethod.POST, "/lineup/submit").permitAll()
            .antMatchers(HttpMethod.GET, "/player/team").permitAll()
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
