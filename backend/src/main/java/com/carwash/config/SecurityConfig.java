package com.carwash.config;

import com.carwash.security.JwtAccessDeniedHandler;
import com.carwash.security.JwtAuthenticationEntryPoint;
import com.carwash.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    /** 允許跨來源呼叫的前端網域（逗號分隔），由 app.cors.allowed-origins / CORS_ALLOWED_ORIGINS 設定 */
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    /** 不需要登入即可存取的公開端點 */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/members/register",
            "/api/members/login",
            "/api/admin/login",
            "/api/payments/newebpay/notify",   // 藍新 NotifyURL，無 JWT，靠 TradeSha 驗證
            "/api/payments/newebpay/return",   // 藍新 ReturnURL，使用者瀏覽器導回
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
                        // 手動標記付款成功/失敗只有後台能用，避免會員自己把付款單改成已付款
                        .requestMatchers("/api/payments/callback/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/payments/reservations/*/cash/collect").hasAnyRole("ADMIN", "STAFF")
                        // 後台管理相關端點僅限 ADMIN / STAFF
                        // 營收報表涉及財務數字，只開放給 ADMIN，STAFF 不行（必須放在 /api/admin/** 之前）
                        .requestMatchers("/api/admin/reports/**").hasRole("ADMIN")
                        // 帳號管理（建立後台帳號）只有 ADMIN
                        .requestMatchers("/api/admin/accounts/**").hasRole("ADMIN")
                        // EMPLOYEE（一般員工）只開放查看總班表，其他後台端點一律不行
                        .requestMatchers(HttpMethod.GET, "/api/admin/schedules", "/api/closed-dates").hasAnyRole("ADMIN", "STAFF", "EMPLOYEE", "MEMBER")
                        .requestMatchers("/api/admin/**", "/api/employees/**").hasAnyRole("ADMIN", "STAFF")
                        // 預約的確認／完成是後台作業；跨會員的日期區間查詢會洩漏所有人的預約
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/*/confirm", "/api/reservations/*/complete").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/by-date-range").hasAnyRole("ADMIN", "STAFF")
                        // 建立優惠券與發放只有後台能做，否則會員可以自己印券、發券給自己
                        .requestMatchers(HttpMethod.POST, "/api/coupons", "/api/coupons/*/issue", "/api/coupons/*/issue-batch").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/services/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/services/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/services/**").hasAnyRole("ADMIN", "STAFF")
                        // 服務項目封面圖只有後台可以上傳；評價附圖/會員大頭照登入即可，走下面的 anyRequest()
                        .requestMatchers(HttpMethod.POST, "/api/files/service-images").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/coupons/**").hasAnyRole("ADMIN", "STAFF", "MEMBER")
                        // 其餘會員相關端點（預約、付款、評價、車輛）；明確排除 EMPLOYEE，
                        // 否則一般員工的 id 會被會員端 API 當成 memberId 使用
                        .anyRequest().hasAnyRole("ADMIN", "STAFF", "MEMBER"))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
