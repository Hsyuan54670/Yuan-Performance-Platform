package com.yuan.common.result;

import com.yuan.common.constant.HttpStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class R<T> {

    private int code;
    private String message;
    private T data;

    public R() {
    }

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> success(T data) {
        return new R<>(HttpStatus.SUCCESS, "success", data);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

}