package com.yuan.common.constant;

public class CommonConstant {
    // JWT 相关常量
    //1.JWT密钥  长度要求:32字节以上
    public static final String SECRET_KEY_STRING = "Secret-Key-For-JWT-2026-Web-Yuan-Performance-Plat-Project-054670";
    //2.JWT过期时间:24小时
    public static final long JWT_EXPIRATION_TIME = 24 * 60 * 60 * 1000;
    //3.JWT刷新令牌过期时间:7天
    public static final long JWT_REFRESH_EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000;


    // Redis相关常量
    //1.用户信息缓存前缀
    public static final String REDIS_USER_INFO = "yuan:auth:userinfo:";
    //2.token黑名单前缀
    public static final String REDIS_BLACKLIST_TOKEN = "yuan:auth:blacklist:token:";
    public static final String REDIS_BLACKLIST_REFRESH_TOKEN = "yuan:auth:blacklist:refresh_token:";

    // JMeter相关常量
    //1. JMeter脚本存放目录
    public static final String JMETER_SCRIPTS_DIR = "E:/AI/workspace/projects/yuan/jmeter/scripts/";
    //2. JMeter结果文件存放目录
    public static final String JMETER_RESULTS_DIR = "E:/AI/workspace/projects/yuan/jmeter/results/";

    // 其它常量
    //1.进程等待时间
    public static final long WAIT_TIME = 3;

}