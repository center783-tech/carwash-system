package com.carwash.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 服務項目（精緻洗車／打蠟／鍍膜...）。
 * 命名為 ServiceItem 而非 Service，避免與 Spring 的 @Service 語意混淆。
 */
@Entity
@Table(name = "services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(length = 30)
    private String category;

    /** 服務項目封面圖網址，由 FileStorageService.uploadServiceImage() 上傳後取得 */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer status = 1;

    /** 是否為組合優惠；組合的 price 是優惠價，時長為各單項加總 */
    @Column(name = "is_bundle", nullable = false)
    @Builder.Default
    private Integer bundle = 0;

    /** 組合包含的單項服務 id，逗號分隔（例如 "1,3,4"）；非組合為 null */
    @Column(name = "bundle_item_ids", length = 255)
    private String bundleItemIds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany(mappedBy = "serviceItems")
    @Builder.Default
    private Set<Employee> employees = new HashSet<>();
}
