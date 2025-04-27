package com.meet5.dao;

import com.meet5.pojo.Like;
import com.meet5.pojo.LikesLog;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Mapper
public interface LikeLogDAO {
    void insert(Like like);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void batchInsertLikes(List<LikesLog> likesList);

    long countTotalRecords();
}
