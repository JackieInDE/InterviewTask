package com.meet5.dao;


import com.meet5.common.enums.UserStatus;
import com.meet5.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserDAO {

    void insert(User user);

    void updateStatus(@Param("userId") Long userId, @Param("userStatus") UserStatus userStatus);

    List<User> selectNormalUsersByIdList(List<Long> allUserIdList);
}
