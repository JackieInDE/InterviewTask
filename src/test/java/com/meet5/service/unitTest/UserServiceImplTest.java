package com.meet5.service.unitTest;

import com.meet5.common.enums.LikeStatus;
import com.meet5.common.enums.UserStatus;
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
import org.mockito.*;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserDAO userDAO;
    @Mock
    private LikeDAO likeDAO;
    @Mock
    private LikeLogDAO likeLogDAO;
    @Mock
    private VisitDAO visitDAO;
    @Mock
    private RiskManagementService riskManagementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void recordVisit_nullRequest_throwsException() {
        assertThrows(NullPointerException.class, () -> userService.recordVisit(null));
    }

    @Test
    void recordVisit_nullVisitorId_throwsException() {
        VisitRequest request = new VisitRequest();
        request.setTargetId(2L);
        assertThrows(NullPointerException.class, () -> userService.recordVisit(request));
    }

    @Test
    void recordVisit_nullTargetId_throwsException() {
        VisitRequest request = new VisitRequest();
        request.setVisitorId(1L);
        assertThrows(NullPointerException.class, () -> userService.recordVisit(request));
    }

    @Test
    void recordVisit_sameVisitorAndTarget_noAction() {
        VisitRequest request = new VisitRequest();
        request.setVisitorId(1L);
        request.setTargetId(1L);

        userService.recordVisit(request);

        verify(visitDAO, never()).recordVisit(anyLong(), anyLong());
        verify(riskManagementService, never()).checkSensitiveBehavior(anyLong());
    }

    @Test
    void recordVisit_validRequest_recordsVisitAndChecksRisk() {
        VisitRequest request = new VisitRequest();
        request.setVisitorId(1L);
        request.setTargetId(2L);

        when(riskManagementService.checkSensitiveBehavior(1L)).thenReturn(false);

        userService.recordVisit(request);

        verify(visitDAO).recordVisit(1L, 2L);
        verify(riskManagementService).checkSensitiveBehavior(1L);
        verify(userDAO, never()).updateStatus(anyLong(), any());
    }

    @Test
    void recordVisit_sensitiveBehavior_updatesUserStatus() {
        VisitRequest request = new VisitRequest();
        request.setVisitorId(1L);
        request.setTargetId(2L);

        when(riskManagementService.checkSensitiveBehavior(1L)).thenReturn(true);

        userService.recordVisit(request);

        verify(userDAO).updateStatus(1L, UserStatus.FRAUD);
    }


    @Test
    void recordLike_nullRequest_throwsException() {
        assertThrows(NullPointerException.class, () -> userService.recordLike(null));
    }

    @Test
    void recordLike_nullLikerId_throwsException() {
        LikeRequest request = new LikeRequest();
        request.setTargetId(2L);
        assertThrows(NullPointerException.class, () -> userService.recordLike(request));
    }

    @Test
    void recordLike_nullTargetId_throwsException() {
        LikeRequest request = new LikeRequest();
        request.setLikerId(1L);
        assertThrows(NullPointerException.class, () -> userService.recordLike(request));
    }

    @Test
    void recordLike_existingLike_cancelsLike() {
        LikeRequest request = new LikeRequest();
        request.setLikerId(1L);
        request.setTargetId(2L);

        Like existingLike = Like.builder().likerId(1L).targetId(2L).status(LikeStatus.LIKED).build();
        when(likeDAO.selectLikeByLikeRequest(request)).thenReturn(existingLike);

        userService.recordLike(request);

        assertEquals(LikeStatus.CANCELED, existingLike.getStatus());
        verify(likeDAO).updateStatus(existingLike);
        verify(likeLogDAO).insert(existingLike);
    }

    @Test
    void recordLike_newLike_recordsLikeAndChecksRisk() {
        LikeRequest request = new LikeRequest();
        request.setLikerId(1L);
        request.setTargetId(2L);

        when(likeDAO.selectLikeByLikeRequest(request)).thenReturn(null);
        when(riskManagementService.checkSensitiveBehavior(1L)).thenReturn(false);

        userService.recordLike(request);

        verify(likeDAO).recordLike(1L, 2L);
        verify(likeLogDAO).insert(any(Like.class));
        verify(riskManagementService).checkSensitiveBehavior(1L);
    }

    @Test
    void recordLike_sensitiveBehavior_updatesUserStatus() {
        LikeRequest request = new LikeRequest();
        request.setLikerId(1L);
        request.setTargetId(2L);

        when(likeDAO.selectLikeByLikeRequest(request)).thenReturn(null);
        when(riskManagementService.checkSensitiveBehavior(1L)).thenReturn(true);

        userService.recordLike(request);

        verify(userDAO).updateStatus(1L, UserStatus.FRAUD);
    }


    @Test
    void getLastMonthVisitors_noVisits_returnsEmptyList() {
        when(visitDAO.findRecentVisits(anyLong(), any())).thenReturn(Collections.emptyList());

        List<UserDto> result = userService.getLastMonthVisitors(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getLastMonthVisitors_visitsAggregatedPerDayAndSorted() {
        Visit visit1 = new Visit(2L, 1L, LocalDateTime.now().minusDays(2));
        Visit visit2 = new Visit(2L, 1L, LocalDateTime.now().minusDays(1));
        Visit visit3 = new Visit(3L, 1L, LocalDateTime.now().minusDays(3));
        Visit visit4 = new Visit(3L, 1L, LocalDateTime.now().minusDays(3).minusHours(5));

        List<Visit> visits = List.of(visit1, visit2, visit3, visit4);

        when(visitDAO.findRecentVisits(eq(1L), any())).thenReturn(visits);

        User user2 = new User();
        user2.setId(2L);
        User user3 = new User();
        user3.setId(3L);

        when(userDAO.selectNormalUsersByIdList(anyList())).thenReturn(List.of(user2, user3));

        List<UserDto> result = userService.getLastMonthVisitors(1L);

        assertEquals(3, result.size());

        assertTrue(result.get(0).getVisitedTime().isAfter(result.get(1).getVisitedTime()));

        assertEquals(2L, result.get(0).getId());
        assertEquals(visit2.getVisitedTime(), result.get(0).getVisitedTime());


        assertEquals(3L, result.get(2).getId());
        assertEquals(visit3.getVisitedTime(), result.get(2).getVisitedTime());
    }

    @Test
    void getLastMonthVisitors_visitsWithNullFields_filteredOut() {
        Visit invalidVisit = new Visit(null, null, null);
        List<Visit> visits = List.of(invalidVisit);

        when(visitDAO.findRecentVisits(eq(1L), any())).thenReturn(visits);

        List<UserDto> result = userService.getLastMonthVisitors(1L);

        assertTrue(result.isEmpty());
    }
}
