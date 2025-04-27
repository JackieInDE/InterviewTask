package com.meet5.pojo;

import jakarta.validation.constraints.NotNull;
import lombok.*;


import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Visit {
    private Long id;
    @NotNull
    private Long visitorId;
    @NotNull
    private Long targetId;
    private LocalDateTime visitedTime;

    public Visit(Long visitorId, Long targetId, LocalDateTime visitedTime) {
        this.visitorId = visitorId;
        this.targetId = targetId;
        this.visitedTime = visitedTime;
    }

}

