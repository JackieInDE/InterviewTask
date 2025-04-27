package com.meet5.service;

import com.meet5.pojo.LikesLog;

import java.util.List;

public interface BatchInsertService {
     void batchInsert(List<LikesLog> likesLogs);
}
