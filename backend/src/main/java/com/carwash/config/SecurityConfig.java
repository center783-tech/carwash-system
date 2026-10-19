package com.carwash.config;

import com.carwash.security.JwtAccessDeniedHandler;
import com.carwash.security.JwtAuthenticationEntryPoint;
import com.carwash.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    /** 不需要登入即可存取的公開端點 */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/members/register",
            "/api/members/login",
            "/api/admin/login",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // 服務項目清單開放未登入瀏覽（洗車服務瀏覽功能）
                        .requestMatchers(HttpMethod.GET, "/api/services/**").permitAll()
                        // 後台管理相關端點僅限 ADMIN / STAFF
                        .requestMatchers("/api/admin/**", "/api/employees/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/services/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/services/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/services/**").hasAnyRole("ADMIN", "STAFF")
                        // 服務項目封面圖只有後台可以上傳；評價附圖/會員大頭照登入即可，走下面的 anyRequest()
                        .requestMatchers(HttpMethod.POST, "/api/files/service-images").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/coupons/**").hasAnyRole("ADMIN", "STAFF", "MEMBER")
                        // 其餘會員相關端點（預約、付款、評價、車輛）只要登入即可（MEMBER 或 ADMIN）
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // 正式環境請改成白名單網域
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
