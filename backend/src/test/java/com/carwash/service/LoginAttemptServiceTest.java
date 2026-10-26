package com.carwash.service;

import com.carwash.constant.RedisKeyConstants;
import com.carwash.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    private static final String KEY = RedisKeyConstants.loginFailCountKey("member:alice");

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void checkNotLocked_belowLimit_passes() {
        when(valueOps.get(KEY)).thenReturn(String.valueOf(RedisKeyConstants.LOGIN_MAX_FAILS - 1));

        assertThatCode(() -> service.checkNotLocked(LoginAttemptService.SCOPE_MEMBER, "alice"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkNotLocked_atLimit_throws() {
        when(valueOps.get(KEY)).thenReturn(String.valueOf(RedisKeyConstants.LOGIN_MAX_FAILS));

        assertThatThrownBy(() -> service.checkNotLocked(LoginAttemptService.SCOPE_MEMBER, "alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登入失敗次數過多");
    }

    @Test
    void checkNotLocked_normalizesUsername() {
        when(valueOps.get(KEY)).thenReturn(String.valueOf(RedisKeyConstants.LOGIN_MAX_FAILS));

        assertThatThrownBy(() -> service.checkNotLocked(LoginAttemptService.SCOPE_MEMBER, "  ALICE "))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void checkNotLocked_redisDown_failsOpen() {
        when(valueOps.get(KEY)).thenThrow(new RedisConnectionFailureException("down"));

        assertThatCode(() -> service.checkNotLocked(LoginAttemptService.SCOPE_MEMBER, "alice"))
                .doesNotThrowAnyException();
    }

    @Test
    void recordFailure_firstFailure_setsExpiry() {
        when(valueOps.increment(KEY)).thenReturn(1L);

        service.recordFailure(LoginAttemptService.SCOPE_MEMBER, "alice");

        verify(redis).expire(KEY, Duration.ofSeconds(RedisKeyConstants.LOGIN_FAIL_COUNT_TTL_SECONDS));
    }

    @Test
    void recordFailure_laterFailure_doesNotExtendExpiry() {
        when(valueOps.increment(KEY)).thenReturn(3L);

        service.recordFailure(LoginAttemptService.SCOPE_MEMBER, "alice");

        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void scopesAreIndependent() {
        when(valueOps.get(RedisKeyConstants.loginFailCountKey("admin:alice"))).thenReturn(null);

        assertThatCode(() -> service.checkNotLocked(LoginAttemptService.SCOPE_ADMIN, "alice"))
                .doesNotThrowAnyException();
    }

    @Test
    void reset_deletesKey() {
        service.reset(LoginAttemptService.SCOPE_MEMBER, "alice");

        verify(redis).delete(KEY);
    }
}
