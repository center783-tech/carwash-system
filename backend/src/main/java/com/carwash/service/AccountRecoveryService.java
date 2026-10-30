package com.carwash.service;

/**
 * 忘記帳號 / 忘記密碼：寄出一次性連結到會員 Email，點開連結後才能取得帳號或重設密碼。
 * 密碼以 BCrypt 雜湊儲存、無法還原，所以「找回密碼」是重設成新密碼。
 */
public interface AccountRecoveryService {

    /** 寄出「查詢帳號」連結；Email 未註冊時靜默略過，避免被用來探測哪些 Email 已註冊 */
    void sendUsernameLink(String email);

    /** 憑連結 token 取得帳號，token 只能用一次 */
    String consumeUsernameToken(String token);

    /** 寄出「重設密碼」連結；Email 未註冊時靜默略過 */
    void sendPasswordResetLink(String email);

    /** 憑連結 token 設定新密碼，token 只能用一次 */
    void resetPassword(String token, String newPassword);
}
