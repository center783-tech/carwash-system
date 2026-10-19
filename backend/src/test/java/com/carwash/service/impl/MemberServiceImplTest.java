package com.carwash.service.impl;

import com.carwash.entity.Member;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberServiceImpl memberService;

    private Member sampleMember;

    @BeforeEach
    void setUp() {
        sampleMember = Member.builder()
                .id(1L)
                .username("testuser")
                .password("encoded-password")
                .email("test@example.com")
                .phone("0912345678")
                .status(1)
                .build();
    }

    @Test
    void register_success() {
        Member newMember = Member.builder()
                .username("newuser")
                .password("rawPassword123")
                .email("new@example.com")
                .phone("0900000000")
                .build();

        when(memberRepository.existsByUsername("newuser")).thenReturn(false);
        when(memberRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword123")).thenReturn("encoded");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Member result = memberService.register(newMember);

        assertThat(result.getPassword()).isEqualTo("encoded");
        assertThat(result.getStatus()).isEqualTo(1);
        verify(memberRepository).save(newMember);
    }

    @Test
    void register_duplicateUsername_throwsBusinessException() {
        Member newMember = Member.builder().username("testuser").email("x@example.com").build();
        when(memberRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> memberService.register(newMember))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("帳號已被使用");

        verify(memberRepository, never()).save(any());
    }

    @Test
    void register_duplicateEmail_throwsBusinessException() {
        Member newMember = Member.builder().username("uniqueuser").email("test@example.com").build();
        when(memberRepository.existsByUsername("uniqueuser")).thenReturn(false);
        when(memberRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.register(newMember))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email 已被註冊");
    }

    @Test
    void login_success() {
        when(memberRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleMember));
        when(passwordEncoder.matches("rawPassword", "encoded-password")).thenReturn(true);

        Member result = memberService.login("testuser", "rawPassword");

        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    void login_wrongPassword_throwsBusinessException() {
        when(memberRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleMember));
        when(passwordEncoder.matches("wrongPassword", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> memberService.login("testuser", "wrongPassword"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("帳號或密碼錯誤");
    }

    @Test
    void login_disabledAccount_throwsBusinessException() {
        sampleMember.setStatus(0);
        when(memberRepository.findByUsername("testuser")).thenReturn(Optional.of(sampleMember));
        when(passwordEncoder.matches("rawPassword", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> memberService.login("testuser", "rawPassword"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被停用");
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changePassword_wrongOldPassword_throwsBusinessException() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(passwordEncoder.matches("wrongOld", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> memberService.changePassword(1L, "wrongOld", "newPassword123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("原密碼不正確");
    }
}
