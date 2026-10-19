package com.carwash.service.impl;

import com.carwash.constant.ReservationStatus;
import com.carwash.entity.Member;
import com.carwash.entity.Reservation;
import com.carwash.entity.Review;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.MemberRepository;
import com.carwash.repository.ReservationRepository;
import com.carwash.repository.ReviewRepository;
import com.carwash.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;

    @Override
    public Review create(Long reservationId, Long memberId, Integer rating, String comment, String imageUrl) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Reservation", reservationId));
        if (reservation.getStatus() != ReservationStatus.COMPLETED) {
            throw new BusinessException("只有已完成的預約才能評價");
        }
        if (reviewRepository.findByReservationId(reservationId).isPresent()) {
            throw new BusinessException("此預約已經評價過了");
        }
        if (rating < 1 || rating > 5) {
            throw new BusinessException("評分須介於 1 到 5 之間");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> ResourceNotFoundException.of("Member", memberId));

        Review review = Review.builder()
                .reservation(reservation)
                .member(member)
                .rating(rating)
                .comment(comment)
                .imageUrl(imageUrl)
                .build();
        return reviewRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Review> listByMember(Long memberId, Pageable pageable) {
        return reviewRepository.findByMemberId(memberId, pageable);
    }
}
