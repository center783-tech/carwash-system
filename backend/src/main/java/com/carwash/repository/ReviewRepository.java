package com.carwash.repository;

import com.carwash.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByReservationId(Long reservationId);

    Page<Review> findByMemberId(Long memberId, Pageable pageable);
}
