package com.meet5.service.integrationTest;

import com.meet5.common.enums.Gender;
import com.meet5.common.enums.LikeStatus;
import com.meet5.pojo.dto.UserDto;
import com.meet5.service.impl.UserServiceImpl;
import com.meet5.dao.*;
import com.meet5.pojo.Like;
import com.meet5.pojo.User;
import com.meet5.pojo.Visit;
import com.meet5.pojo.request.LikeRequest;
import com.meet5.pojo.request.VisitRequest;
import com.meet5.service.RiskManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for UserServiceImpl.
 */
@SpringBootTest
public class UserServiceImplIntegrationTest {

    @Autowired
    private UserServiceImpl userService;

    @MockBean
    private UserDAO userDAO;

    @MockBean
    private LikeDAO likeDAO;

    @MockBean
    private LikeLogDAO likeLogDAO;

    @MockBean
    private VisitDAO visitDAO;

    @MockBean
    private RiskManagementService riskManagementService;

    @BeforeEach
    public void setup() {
        reset(userDAO, likeDAO, likeLogDAO, visitDAO, riskManagementService);
    }


    /**
     * Functional Test:
     * Ensure a normal visit is recorded successfully.
     */
    @Test
    public void testRecordVisit_normalCase() {
        VisitRequest request = new VisitRequest(1L, 2L);

        userService.recordVisit(request);

        verify(visitDAO, times(1)).recordVisit(1L, 2L);
    }

    /**
     * Data Validation Test:
     * Ensure no action when visitorId equals targetId.
     */
    @Test
    public void testRecordVisit_sameUser() {
        VisitRequest request = new VisitRequest(1L, 1L);

        userService.recordVisit(request);

        verify(visitDAO, never()).recordVisit(anyLong(), anyLong());
    }

    /**
     * Data Validation Test:
     * Ensure method throws NullPointerException when request is null.
     */
    @Test
    public void testRecordVisit_nullRequest() {
        assertThrows(NullPointerException.class, () -> userService.recordVisit(null));
    }

    /**
     * Concurrency Test:
     * Verify concurrent calls to recordVisit do not break functionality.
     */
    @Test
    public void testRecordVisit_concurrentAccess() throws InterruptedException, ExecutionException {
        VisitRequest request = new VisitRequest(1L, 2L);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> f1 = executor.submit(() -> userService.recordVisit(request));
        Future<?> f2 = executor.submit(() -> userService.recordVisit(request));

        f1.get();
        f2.get();

        verify(visitDAO, atLeast(2)).recordVisit(1L, 2L);
    }


    /**
     * Functional Test:
     * Ensure a new like is inserted if not already liked.
     */
    @Test
    public void testRecordLike_newLike() {
        LikeRequest request = new LikeRequest(1L, 2L);

        when(likeDAO.selectLikeByLikeRequest(any())).thenReturn(null);

        userService.recordLike(request);

        verify(likeDAO, times(1)).recordLike(1L, 2L);
        verify(likeLogDAO, times(1)).insert(any(Like.class));
    }

    /**
     * Functional Test:
     * Ensure an existing like is updated to canceled status.
     */
    @Test
    public void testRecordLike_existingLike() {
        Like existingLike = Like.builder()
                .likerId(1L)
                .targetId(2L)
                .status(LikeStatus.LIKED)
                .build();

        LikeRequest request = new LikeRequest(1L, 2L);
        when(likeDAO.selectLikeByLikeRequest(any())).thenReturn(existingLike);

        userService.recordLike(request);

        verify(likeDAO, times(1)).updateStatus(existingLike);
        verify(likeLogDAO, times(1)).insert(existingLike);
    }

    /**
     * Data Validation Test:
     * Ensure method throws NullPointerException if request is null.
     */
    @Test
    public void testRecordLike_nullRequest() {
        assertThrows(NullPointerException.class, () -> userService.recordLike(null));
    }

    /**
     * Concurrency Test:
     * Verify concurrent likes are handled properly.
     */
    @Test
    public void testRecordLike_concurrentAccess() throws InterruptedException, ExecutionException {
        LikeRequest request = new LikeRequest(1L, 2L);

        when(likeDAO.selectLikeByLikeRequest(any())).thenReturn(null);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> f1 = executor.submit(() -> userService.recordLike(request));
        Future<?> f2 = executor.submit(() -> userService.recordLike(request));

        f1.get();
        f2.get();

        verify(likeDAO, atLeast(2)).recordLike(1L, 2L);
        verify(likeLogDAO, atLeast(2)).insert(any(Like.class));
    }


    /**
     * Functional Test:
     * Ensure recent visitors are returned correctly.
     */
    @Test
    public void testGetLastMonthVisitors_normalCase() {
        List<Visit> visits = generateVisits(15);
        when(visitDAO.findRecentVisits(anyLong(), any())).thenReturn(visits);

        List<User> users = generateUsers(15);
        when(userDAO.selectNormalUsersByIdList(anyList())).thenReturn(users);

        List<UserDto> result = userService.getLastMonthVisitors(1L);

        assertNotNull(result);
        assertTrue(result.size() <= UserServiceImpl.MAX_VISITORS);
    }

    /**
     * Boundary Test:
     * Verify behavior when no visitors found.
     */
    @Test
    public void testGetLastMonthVisitors_noVisits() {
        when(visitDAO.findRecentVisits(anyLong(), any())).thenReturn(Collections.emptyList());

        List<UserDto> result = userService.getLastMonthVisitors(1L);

        assertTrue(result.isEmpty());
    }

    /**
     * Concurrency Test:
     * Verify concurrent access to get visitors.
     */
    @Test
    public void testGetLastMonthVisitors_concurrentAccess() throws InterruptedException, ExecutionException {
        List<Visit> visits = generateVisits(20);
        when(visitDAO.findRecentVisits(anyLong(), any())).thenReturn(visits);

        List<User> users = generateUsers(20);
        when(userDAO.selectNormalUsersByIdList(anyList())).thenReturn(users);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<List<UserDto>> f1 = executor.submit(() -> userService.getLastMonthVisitors(1L));
        Future<List<UserDto>> f2 = executor.submit(() -> userService.getLastMonthVisitors(1L));

        assertNotNull(f1.get());
        assertNotNull(f2.get());
    }

    /**
     * Helper method to generate dummy Visits.
     */
    private List<Visit> generateVisits(int count) {
        List<Visit> visits = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Visit visit = new Visit();
            visit.setVisitorId((long) i + 1);
            visit.setVisitedTime(LocalDateTime.now().minusDays(i));
            visits.add(visit);
        }
        return visits;
    }

    /**
     * Helper method to generate dummy Users.
     */
    private List<User> generateUsers(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    User user = new User();
                    user.setId((long) i + 1);
                    user.setBirthday(LocalDate.now());
                    user.setGender(Gender.MAN);
                    user.setName(String.valueOf((long) i + 1));
                    user.setLocationId(i);
                    return user;
                }).collect(Collectors.toList());
    }
}
