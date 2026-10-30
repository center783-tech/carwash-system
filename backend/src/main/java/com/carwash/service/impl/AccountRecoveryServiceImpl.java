package com.carwash.service.impl;

import com.carwash.constant.RedisKeyConstants;
import com.carwash.entity.Member;
import com.carwash.exception.BusinessException;
import com.carwash.repository.MemberRepository;
import com.carwash.service.AccountRecoveryService;
import com.carwash.service.LoginAttemptService;
import com.carwash.service.MailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountRecoveryServiceImpl implements AccountRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(AccountRecoveryServiceImpl.class);

    private static final String TYPE_USERNAME = "username";
    private static final String TYPE_PASSWORD = "password";
    private static final String INVALID_LINK = "連結已失效或已使用過，請重新申請";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final MailService mailService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.base-url:http://localhost:5300/carwash}")
    private String baseUrl;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public void sendUsernameLink(String email) {
        sendLink(email, TYPE_USERNAME);
    }

    @Override
    public void sendPasswordResetLink(String email) {
        sendLink(email, TYPE_PASSWORD);
    }

    private void sendLink(String email, String type) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        String cooldownKey = RedisKeyConstants.recoverCooldownKey(type, sha256(normalized));

        // 冷卻鎖放在查會員之前：不論 Email 有沒有註冊行為都一樣，也能擋住狂點寄信
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(cooldownKey, "1",
                Duration.ofSeconds(RedisKeyConstants.RECOVER_COOLDOWN_SECONDS));
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException("寄送過於頻繁，請稍後再試");
        }

        Member member = memberRepository.findByEmail(normalized).orElse(null);
        if (member == null || member.getStatus() == 0) {
            return;
        }

        String token = newToken();
        String tokenKey = TYPE_USERNAME.equals(type)
                ? RedisKeyConstants.recoverUsernameTokenKey(sha256(token))
                : RedisKeyConstants.resetPasswordTokenKey(sha256(token));
        stringRedisTemplate.opsForValue().set(tokenKey, String.valueOf(member.getId()),
                Duration.ofSeconds(RedisKeyConstants.RECOVER_TOKEN_TTL_SECONDS));

        long minutes = RedisKeyConstants.RECOVER_TOKEN_TTL_SECONDS / 60;
        try {
            if (TYPE_USERNAME.equals(type)) {
                mailService.sendEmail(member.getEmail(), "【洗車預約系統】查詢帳號",
                        "您好，\n\n請點擊下列連結查看您的帳號（" + minutes + " 分鐘內有效，且只能使用一次）：\n"
                                + baseUrl + "/recover-username?token=" + token
                                + "\n\n若這不是您本人的操作，請忽略此信件。");
            } else {
                mailService.sendEmail(member.getEmail(), "【洗車預約系統】重設密碼",
                        "您好 " + member.getUsername() + "，\n\n請點擊下列連結設定新密碼（" + minutes
                                + " 分鐘內有效，且只能使用一次）：\n"
                                + baseUrl + "/reset-password?token=" + token
                                + "\n\n若這不是您本人的操作，請忽略此信件，您的密碼不會被更動。");
            }
        } catch (MailException e) {
            // 寄信失敗不可讓呼叫端知道，否則等於洩漏「這個 Email 有註冊」；只記 log 並作廢 token
            logger.error("Failed to send {} recovery mail. memberId={}", type, member.getId(), e);
            stringRedisTemplate.delete(tokenKey);
            stringRedisTemplate.delete(cooldownKey);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String consumeUsernameToken(String token) {
        return consume(RedisKeyConstants.recoverUsernameTokenKey(sha256(token))).getUsername();
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Member member = consume(RedisKeyConstants.resetPasswordTokenKey(sha256(token)));
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
        // 能走到這裡就是本人，順便解除因忘記密碼亂試而造成的登入鎖定
        loginAttemptService.reset(LoginAttemptService.SCOPE_MEMBER, member.getUsername());
    }

    /** getAndDelete 保證 token 只能被一個請求取走 */
    private Member consume(String tokenKey) {
        String memberId = stringRedisTemplate.opsForValue().getAndDelete(tokenKey);
        if (memberId == null) {
            throw new BusinessException(INVALID_LINK);
        }
        return memberRepository.findById(Long.valueOf(memberId))
                .filter(m -> m.getStatus() != 0)
                .orElseThrow(() -> new BusinessException(INVALID_LINK));
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
