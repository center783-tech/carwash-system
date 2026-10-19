package com.carwash.service;

import com.carwash.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    Review create(Long reservationId, Long memberId, Integer rating, String comment, String imageUrl);

    Page<Review> listByMember(Long memberId, Pageable pageable);
}
