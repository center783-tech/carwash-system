package com.carwash.service.impl;

import com.carwash.constant.ReservationStatus;
import com.carwash.entity.Member;
import com.carwash.entity.Reservation;
import com.carwash.entity.Review;
import com.carwash.exception.BusinessException;
import com.carwash.repository.MemberRepository;
import com.carwash.repository.ReservationRepository;
import com.carwash.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private MemberRepository memberRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void create_success() {
        Reservation reservation = Reservation.builder().id(1L).status(ReservationStatus.COMPLETED).build();
        Member member = Member.builder().id(1L).username("member1").build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reviewRepository.findByReservationId(1L)).thenReturn(Optional.empty());
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review result = reviewService.create(1L, 1L, 5, "服務很棒", null);

        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getComment()).isEqualTo("服務很棒");
    }

    @Test
    void create_reservationNotCompleted_throwsBusinessException() {
        Reservation reservation = Reservation.builder().id(1L).status(ReservationStatus.PENDING).build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reviewService.create(1L, 1L, 5, "comment", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有已完成的預約才能評價");
    }

    @Test
    void create_alreadyReviewed_throwsBusinessException() {
        Reservation reservation = Reservation.builder().id(1L).status(ReservationStatus.COMPLETED).build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reviewRepository.findByReservationId(1L)).thenReturn(Optional.of(new Review()));

        assertThatThrownBy(() -> reviewService.create(1L, 1L, 4, "comment", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已經評價過了");
    }

    @Test
    void create_invalidRating_throwsBusinessException() {
        Reservation reservation = Reservation.builder().id(1L).status(ReservationStatus.COMPLETED).build();
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reviewRepository.findByReservationId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.create(1L, 1L, 6, "comment", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("評分須介於");
    }
}
