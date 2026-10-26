package com.carwash.service;

import com.carwash.constant.RedisKeyConstants;
import com.carwash.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

/**
 * 登入失敗次數限制：同一帳號在 LOGIN_FAIL_COUNT_TTL_SECONDS 內失敗達 LOGIN_MAX_FAILS 次就暫時鎖定。
 * 會員與後台各用自己的 scope，互不影響。
 * Redis 掛掉時放行（只記 log），避免 Redis 故障讓所有人都無法登入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    public static final String SCOPE_MEMBER = "member";
    public static final String SCOPE_ADMIN = "admin";

    private final StringRedisTemplate stringRedisTemplate;

    /** 已被鎖定就直接丟例外，要在比對密碼「之前」呼叫，鎖定期間連正確密碼都不受理 */
    public void checkNotLocked(String scope, String username) {
        try {
            String count = stringRedisTemplate.opsForValue().get(key(scope, username));
            if (count != null && Long.parseLong(count) >= RedisKeyConstants.LOGIN_MAX_FAILS) {
                throw new BusinessException("登入失敗次數過多，請 "
                        + (RedisKeyConstants.LOGIN_FAIL_COUNT_TTL_SECONDS / 60) + " 分鐘後再試");
            }
        } catch (DataAccessException | NumberFormatException e) {
            log.warn("無法讀取登入失敗次數，略過鎖定檢查 scope={}", scope, e);
        }
    }

    public void recordFailure(String scope, String username) {
        try {
            String key = key(scope, username);
            Long fails = stringRedisTemplate.opsForValue().increment(key);
            // 只在第一次失敗時設 TTL，鎖定時間從第一次失敗起算，不會被持續嘗試無限延長
            if (fails != null && fails == 1L) {
                stringRedisTemplate.expire(key, Duration.ofSeconds(RedisKeyConstants.LOGIN_FAIL_COUNT_TTL_SECONDS));
            }
        } catch (DataAccessException e) {
            log.warn("無法記錄登入失敗次數 scope={}", scope, e);
        }
    }

    public void reset(String scope, String username) {
        try {
            stringRedisTemplate.delete(key(scope, username));
        } catch (DataAccessException e) {
            log.warn("無法清除登入失敗次數 scope={}", scope, e);
        }
    }

    /** 資料庫比對帳號不分大小寫、忽略尾端空白，key 也要正規化，否則換個大小寫就能繞過計數 */
    private String key(String scope, String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return RedisKeyConstants.loginFailCountKey(scope + ":" + normalized);
    }
}
