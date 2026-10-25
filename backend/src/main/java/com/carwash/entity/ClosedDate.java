package com.carwash.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 休息日：後台設定後，該日不開放預約 */
@Entity
@Table(name = "closed_dates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClosedDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "closed_date", nullable = false, unique = true)
    private LocalDate closedDate;

    @Column(length = 100)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
