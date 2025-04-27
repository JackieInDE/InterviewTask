package com.meet5.service.integrationTest;

import com.meet5.dao.LikeLogDAO;
import com.meet5.pojo.LikesLog;
import com.meet5.service.impl.BatchInsertServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for BatchInsertServiceImpl.
 */
@SpringBootTest
public class BatchInsertServiceImplIntegrationTest {

    @Autowired
    private BatchInsertServiceImpl batchInsertService;

    @MockBean
    private LikeLogDAO likeLogDAO;

    @BeforeEach
    public void setup() {
        reset(likeLogDAO);
    }

    /**
     * Functional Test:
     * Verify that valid list is inserted correctly.
     */
    @Test
    public void testBatchInsert_withValidData() {
        List<LikesLog> likesLogs = generateLikesLogs(500);

        batchInsertService.batchInsert(likesLogs);

        verify(likeLogDAO, times(1)).batchInsertLikes(anyList());
    }

    /**
     * Functional Test:
     * Verify no insertion happens when the input list is empty.
     */
    @Test
    public void testBatchInsert_withEmptyList() {
        batchInsertService.batchInsert(Collections.emptyList());

        verify(likeLogDAO, never()).batchInsertLikes(anyList());
    }

    /**
     * Data Validation Test:
     * Verify that null entries are filtered out before insertion.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testBatchInsert_withNullEntries() {
        List<LikesLog> likesLogs = new ArrayList<>();
        likesLogs.add(null);
        likesLogs.add(new LikesLog());

        batchInsertService.batchInsert(likesLogs);

        ArgumentCaptor<List<LikesLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(likeLogDAO, times(1)).batchInsertLikes(captor.capture());

        List<LikesLog> capturedLogs = captor.getValue();
        assertEquals(1, capturedLogs.size());
    }

    /**
     * Boundary Test:
     * Verify behavior when list size is exactly equal to batch size.
     */
    @Test
    public void testBatchInsert_exactBatchSize() {
        List<LikesLog> likesLogs = generateLikesLogs(1000);

        batchInsertService.batchInsert(likesLogs);

        verify(likeLogDAO, times(1)).batchInsertLikes(anyList());
    }

    /**
     * Boundary Test:
     * Verify behavior when list size is slightly over batch size.
     */
    @Test
    public void testBatchInsert_overBatchSize() {
        List<LikesLog> likesLogs = generateLikesLogs(1001);

        batchInsertService.batchInsert(likesLogs);

        verify(likeLogDAO, times(2)).batchInsertLikes(anyList());
    }

    /**
     * Concurrency Test:
     * Verify that batch inserts run concurrently without exception.
     */
    @Test
    public void testBatchInsert_concurrentExecution() throws InterruptedException, ExecutionException {
        List<LikesLog> likesLogs = generateLikesLogs(5000);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<?> future1 = executor.submit(() -> batchInsertService.batchInsert(likesLogs));
        Future<?> future2 = executor.submit(() -> batchInsertService.batchInsert(likesLogs));

        future1.get();
        future2.get();

        verify(likeLogDAO, atLeast(10)).batchInsertLikes(anyList());
    }

    /**
     * Helper method to generate a list of LikesLog.
     *
     * @param count number of LikesLog entries to generate
     * @return List of LikesLog
     */
    private List<LikesLog> generateLikesLogs(int count) {
        List<LikesLog> list = new ArrayList<>(count);
        IntStream.range(0, count).forEach(i -> {
            LikesLog log = new LikesLog();
            list.add(log);
        });
        return list;
    }
}

