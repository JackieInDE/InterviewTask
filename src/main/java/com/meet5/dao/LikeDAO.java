package com.meet5.dao;

import com.meet5.pojo.Like;
import com.meet5.pojo.request.LikeRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LikeDAO {
    Like selectLikeByLikeRequest(LikeRequest request);

    Like selectDataByLikeRequest(LikeRequest request);

    void updateStatus(Like like);

    void recordLike(@Param("likerId") Long likerId, @Param("targetId") Long targetId);

    void insert(Like like);
}
