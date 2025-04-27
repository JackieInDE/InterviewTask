package com.meet5.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LikesLog {
    private Long id;
    private Long likerId;
    private Long targetId;
    private LocalDateTime createdTime;
    private Integer status;
}

