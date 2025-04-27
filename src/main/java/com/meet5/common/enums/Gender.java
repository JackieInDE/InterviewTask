package com.meet5.common.enums;

import lombok.Getter;

@Getter
public enum Gender {
    WOMAN(0),
    MAN(1);

    private final int code;

    Gender(int code) {
        this.code = code;
    }

    public static Gender fromCode(int code) {
        for (Gender status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid Gender code: " + code);
    }
}
