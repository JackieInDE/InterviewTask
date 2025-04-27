package com.meet5.pojo;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class UserTest {

    @Test
    void testGetAge() {
        User user = Mockito.mock(User.class);

        LocalDate birthday = LocalDate.of(2000, 4, 1);


        when(user.getBirthday()).thenReturn(birthday);


        User spyUser = Mockito.spy(new User());
        spyUser.setBirthday(birthday);

        OptionalInt age = spyUser.getAge();

        int expectedAge = LocalDate.now().getYear() - 2000;
        if (LocalDate.now().getDayOfYear() < birthday.getDayOfYear()) {
            expectedAge--;
        }
        if (age.isPresent())
            assertEquals(expectedAge, age.getAsInt(), "Age calculation error.");
    }
}
