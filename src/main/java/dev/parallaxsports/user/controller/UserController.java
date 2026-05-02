package dev.parallaxsports.user.controller;

import dev.parallaxsports.user.dto.CurrentUserResponse;
import dev.parallaxsports.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(userService.findCurrentUser(userDetails.getUsername()));
    }

    @GetMapping("email")
    public Boolean emailExists(String email) {
        return userService.existsByEmail(email);
    }
}
