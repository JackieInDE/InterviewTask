package com.meet5.pojo;

import com.meet5.common.enums.LikeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class Like {
    private Long id;
    @NotNull
    private Long likerId;
    @NotNull
    private Long targetId;
    private LocalDateTime likedTime;
    private LikeStatus status;
    private LocalDateTime updatedTime;
}
