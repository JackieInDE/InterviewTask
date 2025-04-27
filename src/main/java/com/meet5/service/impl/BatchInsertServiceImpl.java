package com.meet5.service.impl;

import com.meet5.dao.LikeLogDAO;
import com.meet5.pojo.LikesLog;
import com.meet5.service.BatchInsertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@Service
public class BatchInsertServiceImpl implements BatchInsertService {

    @Autowired
    private LikeLogDAO likeLogDAO;

    private static final int BATCH_SIZE = 1000;

    @Override
    public void batchInsert(@NonNull List<LikesLog> likesLogs) {
        if (likesLogs.isEmpty()) {
            return;
        }

        // Filter out any null entries
        List<LikesLog> validLogs = likesLogs.stream()
                .filter(Objects::nonNull)
                .toList();
        if (validLogs.isEmpty()) {
            return;
        }

        int total = validLogs.size();
        List<CompletableFuture<Void>> futures = IntStream.iterate(0, i -> i < total, i -> i + BATCH_SIZE)
                .mapToObj(start -> {
                    int end = Math.min(start + BATCH_SIZE, total);
                    List<LikesLog> batch = validLogs.subList(start, end);
                    return CompletableFuture.runAsync(() -> likeLogDAO.batchInsertLikes(batch));
                })
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
