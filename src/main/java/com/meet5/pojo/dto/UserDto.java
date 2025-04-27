package com.meet5.pojo.dto;

import com.meet5.common.enums.Gender;
import com.meet5.common.enums.RelationshipStatus;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.OptionalInt;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String name;
    private String job;
    private Gender gender;
    private LocalDate birthday;
    private Integer locationId;
    private Integer age;
    private String accountStatus;
    private RelationshipStatus relationshipStatus;
    private Long profilePictureId;
    private LocalDateTime createdTime;
    private LocalDateTime visitedTime;

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

