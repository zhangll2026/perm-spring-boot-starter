package com.perm.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应封装
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return Result.<T>builder().code(200).message("success").data(data).build();
    }

    public static <T> Result<T> success() {
        return Result.<T>builder().code(200).message("success").build();
    }

    public static <T> Result<T> error(int code, String message) {
        return Result.<T>builder().code(code).message(message).build();
    }

    public static <T> Result<T> error(String message) {
        return Result.<T>builder().code(500).message(message).build();
    }
}
