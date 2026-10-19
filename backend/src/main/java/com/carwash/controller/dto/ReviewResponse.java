package com.carwash.controller.dto;

import com.carwash.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long reservationId;
    private Long memberId;
    private String memberName;
    private Integer rating;
    private String comment;
    private String imageUrl;
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getReservation().getId(),
                review.getMember().getId(),
                review.getMember().getRealName() != null ? review.getMember().getRealName() : review.getMember().getUsername(),
                review.getRating(),
                review.getComment(),
                review.getImageUrl(),
                review.getCreatedAt()
        );
    }
}
