package com.meet5.dao;

import com.meet5.pojo.Visit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VisitDAO {

    List<Visit> findRecentVisits(@Param("userId") long userId, @Param("monthAgo") LocalDateTime monthAgo);


    void recordVisit(@Param("visitorId") Long visitorId, @Param("targetId") Long targetId);

    void insert(Visit visit);
}
