package dev.parallaxsports.user.controller;

import dev.parallaxsports.auth.service.JwtTokenProvider;
import dev.parallaxsports.user.dto.CurrentUserResponse;
import dev.parallaxsports.user.dto.PasswordRequest;
import dev.parallaxsports.user.dto.UpdateEmailRequest;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/email")
    public ResponseEntity<Void> updateEmail(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateEmailRequest emailRequest,
            HttpServletResponse response
            ) {

        System.out.println("Email:" + emailRequest.email() + " newEmail:" + emailRequest.newEmail());
        User user = userService.updateEmail(emailRequest.email(), emailRequest.newEmail());
        userService.refreshToken(user, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate-password")
    public ResponseEntity<Boolean> validatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PasswordRequest passwordRequest
    ) {
        boolean valid = userService.validatePassword(userDetails.getUsername(), passwordRequest.password());
        return ResponseEntity.ok(valid);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PasswordRequest passwordRequest
    ) {
        userService.updatePassword(userDetails.getUsername(), passwordRequest.password());
        return ResponseEntity.noContent().build();
    }
}
