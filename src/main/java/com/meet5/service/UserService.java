package com.meet5.service;

import com.meet5.pojo.dto.UserDto;
import com.meet5.pojo.request.LikeRequest;
import com.meet5.pojo.request.VisitRequest;

import java.util.List;

public interface UserService {
    void recordVisit(VisitRequest request);

    void recordLike(LikeRequest request);

    List<UserDto> getLastMonthVisitors(long userId);
}
