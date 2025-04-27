package com.meet5.service.unitTest;

import com.meet5.dao.LikeLogDAO;
import com.meet5.pojo.LikesLog;
import com.meet5.service.impl.BatchInsertServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BatchInsertServiceImplTest {

    @InjectMocks
    private BatchInsertServiceImpl batchInsertService;

    @Mock
    private LikeLogDAO likeLogDAO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void batchInsert_emptyList_noInsertion() {
        batchInsertService.batchInsert(Collections.emptyList());

        verify(likeLogDAO, never()).batchInsertLikes(anyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    void batchInsert_nullEntries_onlyValidEntriesInserted() {
        LikesLog log1 = new LikesLog();
        LikesLog log2 = null;
        LikesLog log3 = new LikesLog();

        List<LikesLog> input = new ArrayList<>(3);
        input.add(log1);
        input.add(log2);
        input.add(log3);

        batchInsertService.batchInsert(input);

        ArgumentCaptor<List<LikesLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(likeLogDAO, times(1)).batchInsertLikes(captor.capture());

        List<LikesLog> captured = captor.getValue();
        assertEquals(2, captured.size());
        assertTrue(captured.contains(log1));
        assertTrue(captured.contains(log3));
    }

    @Test
    void batchInsert_allNullEntries_noInsertion() {
        List<LikesLog> input = new ArrayList<>();
        input.add(null);
        input.add(null);

        batchInsertService.batchInsert(input);

        verify(likeLogDAO, never()).batchInsertLikes(anyList());
    }

    @Test
    void batchInsert_lessThanBatchSize_oneCall() {
        List<LikesLog> input = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            input.add(new LikesLog());
        }

        batchInsertService.batchInsert(input);

        verify(likeLogDAO, times(1)).batchInsertLikes(input);
    }

    @SuppressWarnings("unchecked")
    @Test
    void batchInsert_moreThanBatchSize_multipleCalls() {
        int totalRecords = 2500;
        List<LikesLog> input = new ArrayList<>();
        for (int i = 0; i < totalRecords; i++) {
            input.add(new LikesLog());
        }

        batchInsertService.batchInsert(input);

        ArgumentCaptor<List<LikesLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(likeLogDAO, times(3)).batchInsertLikes(captor.capture());

        List<List<LikesLog>> allBatches = captor.getAllValues();
        assertEquals(3, allBatches.size());
        int size0 = allBatches.get(0).size();
        int size1 = allBatches.get(1).size();
        int size2 = allBatches.get(2).size();
        assertEquals(2500, size1 + size2 + size0);
    }

    @Test
    void batchInsert_exceptionInAsyncTask_propagates() {
        LikesLog log = new LikesLog();
        List<LikesLog> input = List.of(log);

        doThrow(new RuntimeException("Database error")).when(likeLogDAO).batchInsertLikes(anyList());

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> batchInsertService.batchInsert(input));

        assertTrue(thrown.getMessage().contains("Database error"));
    }
}
