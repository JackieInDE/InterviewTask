package com.meet5.pojo.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VisitRequest {
    @NotNull
    private Long visitorId;
    @NotNull
    private Long targetId;
}
