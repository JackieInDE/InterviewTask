package com.meet5.service.impl;

import com.meet5.common.enums.LikeStatus;
import com.meet5.common.enums.UserStatus;
import com.meet5.dao.*;
import com.meet5.pojo.Like;
import com.meet5.pojo.User;
import com.meet5.pojo.Visit;
import com.meet5.pojo.dto.UserDto;
import com.meet5.pojo.request.LikeRequest;
import com.meet5.pojo.request.VisitRequest;
import com.meet5.service.RiskManagementService;
import com.meet5.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    public static final int MAX_VISITORS = 10;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private LikeDAO likeDAO;
    @Autowired
    private LikeLogDAO likeLogDAO;
    @Autowired
    private VisitDAO visitDAO;

    @Autowired
    private RiskManagementService riskManagementService;


    @Override
    public void recordVisit(@Valid @NotNull VisitRequest request) {
        Objects.requireNonNull(request, "VisitRequest cannot be null");

        Long visitorId = Objects.requireNonNull(request.getVisitorId(), "Visitor ID cannot be null");
        Long targetId = Objects.requireNonNull(request.getTargetId(), "Target ID cannot be null");

        if (visitorId.equals(targetId)) {
            return;
        }

        visitDAO.recordVisit(visitorId, targetId);
        checkSensitiveBehavior(visitorId);
    }

    private void checkSensitiveBehavior(Long userId) {
        if (riskManagementService.checkSensitiveBehavior(userId)) {
            userDAO.updateStatus(userId, UserStatus.FRAUD);
        }
    }

    @Override
    public void recordLike(@Valid @NotNull LikeRequest request) {
        Objects.requireNonNull(request, "LikeRequest cannot be null");

        Long likerId = Objects.requireNonNull(request.getLikerId(), "Liker ID cannot be null");
        Long targetId = Objects.requireNonNull(request.getTargetId(), "Target ID cannot be null");

        Like like = likeDAO.selectLikeByLikeRequest(request);

        if (like != null) {
            like.setStatus(LikeStatus.CANCELED);
            likeDAO.updateStatus(like);
        } else {
            like = Like.builder()
                    .likerId(likerId)
                    .targetId(targetId)
                    .status(LikeStatus.LIKED)
                    .build();
            likeDAO.recordLike(likerId, targetId);
            checkSensitiveBehavior(likerId);
        }
        likeLogDAO.insert(like);
    }

    /**
     * Retrieves the list of visitors who accessed the specified user's profile within the last month.
     *
     * <p>Method workflow:</p>
     * <ul>
     *     <li>Fetch all visit records to the user's profile from the past month.</li>
     *     <li>For each visitor, retain only the latest visit per day.</li>
     *     <li>Sort the visits in descending order of visit time and select the top {@code MAX_VISITORS} entries.</li>
     *     <li>Batch query the corresponding user information based on visitor IDs.</li>
     *     <li>Copy properties to new User objects and attach the visit time as an additional field.</li>
     * </ul>
     *
     * <p>Notes:</p>
     * <ul>
     *     <li>If no valid visit records are found, returns an empty list.</li>
     *     <li>If the user information of some visitors does not exist or the user status becomes Fraud or Deleted, the corresponding records are filtered out.</li>
     * </ul>
     *
     * @param userId the ID of the user whose visitor records are to be retrieved
     * @return a list of visitors (as User objects) along with their most recent visit time
     */
    @Override
    public List<UserDto> getLastMonthVisitors(long userId) {

        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);

        List<Visit> allVisits = visitDAO.findRecentVisits(userId, monthAgo);

        if (allVisits.isEmpty()) {
            return List.of();
        }

        record DayKey(Long visitorId, LocalDate date) {
        }

        Map<DayKey, Visit> latestVisitPerDay = allVisits.stream()
                .filter(v -> v != null
                        && v.getVisitorId() != null
                        && v.getVisitedTime() != null)
                .collect(Collectors.toMap(
                        v -> new DayKey(v.getVisitorId(), v.getVisitedTime().toLocalDate()),
                        Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(Visit::getVisitedTime))));

        List<Visit> topVisits = latestVisitPerDay.values().stream()
                .sorted(Comparator.comparing(Visit::getVisitedTime).reversed())
                .limit(MAX_VISITORS)
                .toList();

        if (topVisits.isEmpty()) {
            return List.of();
        }


        List<Long> visitorIds = topVisits.stream()
                .map(Visit::getVisitorId)
                .distinct()
                .toList();

        Map<Long, User> userMap = userDAO.selectNormalUsersByIdList(visitorIds).stream()
                .filter(u -> u != null && u.getId() != null)
                .collect(Collectors.toMap(
                        User::getId,
                        Function.identity()
                ));

        return topVisits.stream()
                .map(visit -> {
                    User sourceUser = userMap.get(visit.getVisitorId());
                    if (sourceUser == null) {
                        return null;
                    }
                    UserDto copiedUser = new UserDto();
                    BeanUtils.copyProperties(sourceUser, copiedUser);
                    copiedUser.setVisitedTime(visit.getVisitedTime());
                    return copiedUser;
                })
                .filter(Objects::nonNull)
                .toList();
    }

}
