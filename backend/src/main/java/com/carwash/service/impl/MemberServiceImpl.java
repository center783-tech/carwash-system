package com.carwash.service.impl;

import com.carwash.constant.RedisKeyConstants;
import com.carwash.entity.Member;
import com.carwash.exception.BusinessException;
import com.carwash.exception.ResourceNotFoundException;
import com.carwash.repository.MemberRepository;
import com.carwash.service.MailService;
import com.carwash.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final MailService mailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public Member register(Member member) {
        if (memberRepository.existsByUsername(member.getUsername())) {
            throw new BusinessException("帳號已被使用");
        }
        if (memberRepository.existsByEmail(member.getEmail())) {
            throw new BusinessException("Email 已被註冊");
        }
        member.setPassword(passwordEncoder.encode(member.getPassword()));
        member.setStatus(1);
        return memberRepository.save(member);
    }

    @Override
    @Transactional(readOnly = true)
    public Member login(String username, String rawPassword) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("帳號或密碼錯誤"));
        if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new BusinessException("帳號或密碼錯誤");
        }
        if (member.getStatus() == 0) {
            throw new BusinessException("此帳號已被停用");
        }
        return member;
    }

    @Override
    @Transactional(readOnly = true)
    public Member getById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Member", id));
    }

    @Override
    public Member updateProfile(Long id, Member updates) {
        Member member = getById(id);
        member.setRealName(updates.getRealName());
        member.setPhone(updates.getPhone());
        member.setAvatarUrl(updates.getAvatarUrl());
        return memberRepository.save(member);
    }

    @Override
    @Transactional(readOnly = true)
    public void sendPasswordChangeCode(Long id) {
        Member member = getById(id);

        // setIfAbsent 當作冷卻鎖，避免被狂點「寄送驗證碼」把 Gmail 寄信額度耗光
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
                RedisKeyConstants.passwordCodeCooldownKey(id), "1",
                Duration.ofSeconds(RedisKeyConstants.PASSWORD_CODE_COOLDOWN_SECONDS));
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException("驗證碼寄送過於頻繁，請稍後再試");
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        stringRedisTemplate.opsForValue().set(RedisKeyConstants.passwordCodeKey(id), code,
                Duration.ofSeconds(RedisKeyConstants.PASSWORD_CODE_TTL_SECONDS));
        stringRedisTemplate.delete(RedisKeyConstants.passwordCodeFailKey(id));

        try {
            mailService.sendEmail(member.getEmail(), "【洗車預約系統】修改密碼驗證碼",
                    "您好 " + member.getUsername() + "，\n\n您的修改密碼驗證碼為：" + code
                            + "\n驗證碼 " + (RedisKeyConstants.PASSWORD_CODE_TTL_SECONDS / 60)
                            + " 分鐘內有效，請勿將驗證碼告訴任何人。\n\n若這不是您本人的操作，請忽略此信件。");
        } catch (MailException e) {
            // 寄信失敗就把剛產生的驗證碼與冷卻一併清掉，讓使用者可以馬上重試
            stringRedisTemplate.delete(RedisKeyConstants.passwordCodeKey(id));
            stringRedisTemplate.delete(RedisKeyConstants.passwordCodeCooldownKey(id));
            throw new BusinessException("驗證碼寄送失敗，請稍後再試");
        }
    }

    @Override
    public void changePassword(Long id, String oldPassword, String newPassword, String verificationCode) {
        Member member = getById(id);
        verifyPasswordChangeCode(id, verificationCode);
        if (!passwordEncoder.matches(oldPassword, member.getPassword())) {
            throw new BusinessException("原密碼不正確");
        }
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
        // 驗證碼只能用一次
        stringRedisTemplate.delete(RedisKeyConstants.passwordCodeKey(id));
        stringRedisTemplate.delete(RedisKeyConstants.passwordCodeFailKey(id));
    }

    private void verifyPasswordChangeCode(Long id, String inputCode) {
        String codeKey = RedisKeyConstants.passwordCodeKey(id);
        String failKey = RedisKeyConstants.passwordCodeFailKey(id);

        String stored = stringRedisTemplate.opsForValue().get(codeKey);
        if (stored == null) {
            throw new BusinessException("驗證碼已過期或尚未取得，請重新寄送");
        }
        boolean matched = MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8), inputCode.getBytes(StandardCharsets.UTF_8));
        if (matched) {
            return;
        }

        // 6 位數只有一百萬種組合，必須限制錯誤次數，否則可被暴力枚舉
        Long fails = stringRedisTemplate.opsForValue().increment(failKey);
        if (fails != null && fails == 1L) {
            stringRedisTemplate.expire(failKey, Duration.ofSeconds(RedisKeyConstants.PASSWORD_CODE_TTL_SECONDS));
        }
        if (fails != null && fails >= RedisKeyConstants.PASSWORD_CODE_MAX_FAILS) {
            stringRedisTemplate.delete(codeKey);
            stringRedisTemplate.delete(failKey);
            throw new BusinessException("驗證碼錯誤次數過多，請重新寄送驗證碼");
        }
        throw new BusinessException("驗證碼錯誤");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Member> findAll() {
        return memberRepository.findAll();
    }
}
