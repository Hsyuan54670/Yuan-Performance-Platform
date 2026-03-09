package com.yuan.common.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

import static com.yuan.common.constant.CommonConstant.*;

public class JwtUtil {
    // 生成HS256密钥
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());
    // 生成JWT令牌
    public static String generateToken(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .expiration(new Date(System.currentTimeMillis() +JWT_EXPIRATION_TIME )) // 24小时过期
                .signWith(SECRET_KEY, Jwts.SIG.HS256)
                .compact();
    }
    //生成刷新令牌
    public static String generateRefreshToken(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .expiration(new Date(System.currentTimeMillis() + JWT_REFRESH_EXPIRATION_TIME)) // 刷新令牌过期时间更长
                .signWith(SECRET_KEY, Jwts.SIG.HS256)
                .compact();
    }
    // 解析JWT令牌合法性校验
    public static Map<String, Object> parseToken(String token) throws Exception {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 获取JWT令牌剩余有效期
    public static long getRemainingExpiration(String token) throws Exception {
        Date expiration = Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return (expiration.getTime() - System.currentTimeMillis()) ; // 返回剩余毫秒
    }
}