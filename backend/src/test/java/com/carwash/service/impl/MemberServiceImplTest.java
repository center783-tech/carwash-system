package com.carwash.service.impl;

import com.carwash.constant.RedisKeyConstants;
import com.carwash.entity.Member;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.MemberRepository;
import com.carwash.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

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

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private MailService mailService;

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
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RedisKeyConstants.passwordCodeKey(1L))).thenReturn("123456");
        when(passwordEncoder.matches("wrongOld", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> memberService.changePassword(1L, "wrongOld", "newPassword123", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("原密碼不正確");

        verify(memberRepository, never()).save(any());
    }

    @Test
    void changePassword_success_savesNewPasswordAndConsumesCode() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RedisKeyConstants.passwordCodeKey(1L))).thenReturn("123456");
        when(passwordEncoder.matches("oldPassword", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded");

        memberService.changePassword(1L, "oldPassword", "newPassword123", "123456");

        assertThat(sampleMember.getPassword()).isEqualTo("new-encoded");
        verify(memberRepository).save(sampleMember);
        verify(stringRedisTemplate).delete(RedisKeyConstants.passwordCodeKey(1L));
    }

    @Test
    void changePassword_codeExpired_throwsBusinessException() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RedisKeyConstants.passwordCodeKey(1L))).thenReturn(null);

        assertThatThrownBy(() -> memberService.changePassword(1L, "oldPassword", "newPassword123", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已過期");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void changePassword_wrongCode_throwsBusinessExceptionAndCountsFailure() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RedisKeyConstants.passwordCodeKey(1L))).thenReturn("123456");
        when(valueOps.increment(RedisKeyConstants.passwordCodeFailKey(1L))).thenReturn(1L);

        assertThatThrownBy(() -> memberService.changePassword(1L, "oldPassword", "newPassword123", "000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("驗證碼錯誤");

        verify(stringRedisTemplate).expire(eq(RedisKeyConstants.passwordCodeFailKey(1L)), any(Duration.class));
        verify(memberRepository, never()).save(any());
    }

    @Test
    void changePassword_tooManyWrongCodes_invalidatesCode() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RedisKeyConstants.passwordCodeKey(1L))).thenReturn("123456");
        when(valueOps.increment(RedisKeyConstants.passwordCodeFailKey(1L)))
                .thenReturn((long) RedisKeyConstants.PASSWORD_CODE_MAX_FAILS);

        assertThatThrownBy(() -> memberService.changePassword(1L, "oldPassword", "newPassword123", "000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("次數過多");

        verify(stringRedisTemplate).delete(RedisKeyConstants.passwordCodeKey(1L));
    }

    @Test
    void sendPasswordChangeCode_success_storesCodeAndMailsIt() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq(RedisKeyConstants.passwordCodeCooldownKey(1L)), any(), any(Duration.class)))
                .thenReturn(true);

        memberService.sendPasswordChangeCode(1L);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(eq(RedisKeyConstants.passwordCodeKey(1L)), codeCaptor.capture(), any(Duration.class));
        assertThat(codeCaptor.getValue()).matches("\\d{6}");

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendEmail(eq("test@example.com"), any(), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).contains(codeCaptor.getValue());
    }

    @Test
    void sendPasswordChangeCode_withinCooldown_throwsBusinessException() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq(RedisKeyConstants.passwordCodeCooldownKey(1L)), any(), any(Duration.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> memberService.sendPasswordChangeCode(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("過於頻繁");

        verify(mailService, never()).sendEmail(any(), any(), any());
    }

    @Test
    void sendPasswordChangeCode_mailFails_clearsCodeAndCooldown() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq(RedisKeyConstants.passwordCodeCooldownKey(1L)), any(), any(Duration.class)))
                .thenReturn(true);
        doThrow(new MailSendException("smtp down")).when(mailService).sendEmail(any(), any(), any());

        assertThatThrownBy(() -> memberService.sendPasswordChangeCode(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("寄送失敗");

        verify(stringRedisTemplate).delete(RedisKeyConstants.passwordCodeKey(1L));
        verify(stringRedisTemplate).delete(RedisKeyConstants.passwordCodeCooldownKey(1L));
    }
}
