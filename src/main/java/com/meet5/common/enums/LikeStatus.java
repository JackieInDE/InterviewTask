package com.meet5.common.enums;

import lombok.Getter;

@Getter
public enum LikeStatus {
    LIKED(0),
    CANCELED(1);

    private final int code;

    LikeStatus(int code) {
        this.code = code;
    }

    public static LikeStatus fromCode(int code) {
        for (LikeStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid Like status code: " + code);
    }
}
