package com.yuan.common.util;

import com.yuan.common.constant.HttpStatus;
import com.yuan.common.result.R;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class PermissionUtil {

    private PermissionUtil() {
    }

    public static boolean hasPermission(String permissionHeader, String requiredPermission) {
        if (!StringUtils.hasText(requiredPermission)) {
            return true;
        }
        return parsePermissions(permissionHeader).contains(requiredPermission);
    }

    public static Set<String> parsePermissions(String permissionHeader) {
        if (!StringUtils.hasText(permissionHeader)) {
            return Set.of();
        }
        return Arrays.stream(permissionHeader.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    public static <T> R<T> forbidden(String requiredPermission) {
        String message = StringUtils.hasText(requiredPermission)
                ? "缺少权限: " + requiredPermission
                : "无权执行该操作";
        return R.fail(HttpStatus.FORBIDDEN, message);
    }
}
