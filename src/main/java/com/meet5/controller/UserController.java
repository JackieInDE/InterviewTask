package com.meet5.controller;

import com.meet5.pojo.dto.UserDto;
import com.meet5.pojo.request.LikeRequest;
import com.meet5.pojo.request.VisitRequest;
import com.meet5.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    /**
     * Records a profile visit.
     */
    @PostMapping("/visit")
    public ResponseEntity<Void> visit(@Valid @RequestBody VisitRequest request) {
        userService.recordVisit(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Records a profile like.
     */
    @PostMapping("/like")
    public ResponseEntity<Void> like(@Valid @RequestBody LikeRequest request) {
        userService.recordLike(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves visitors of a profile.
     */
    @GetMapping("/{id}/getVisitors")
    public ResponseEntity<List<UserDto>> getLastMonthVisitors(@PathVariable("id") long userId) {
        List<UserDto> visitors = userService.getLastMonthVisitors(userId);
        return ResponseEntity.ok(visitors);
    }
}

