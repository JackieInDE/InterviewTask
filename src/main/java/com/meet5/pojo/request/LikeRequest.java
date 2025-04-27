package com.meet5.pojo.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeRequest {
    @NotNull
    private Long likerId;
    @NotNull
    private Long targetId;
}
