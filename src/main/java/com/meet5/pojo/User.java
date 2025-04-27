package com.meet5.pojo;

import com.meet5.common.enums.Gender;
import com.meet5.common.enums.RelationshipStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.OptionalInt;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long id;
    @NotBlank
    private String name;
    private String job;
    @NotNull
    private Gender gender;
    @NotNull
    private LocalDate birthday;
    @NotNull
    private Integer locationId;
    private Integer age;
    private String accountStatus;
    private RelationshipStatus relationshipStatus;
    private Long profilePictureId;
    private LocalDateTime createdTime;
    private String createdBy;
    private LocalDateTime updatedTime;
    private String updatedBy;

    public OptionalInt getAge() {
        if (birthday == null) {
            return OptionalInt.empty();
        }
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (birthday.isAfter(today)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Period.between(birthday, today).getYears());
    }
}

