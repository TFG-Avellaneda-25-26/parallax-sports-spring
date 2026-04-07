package dev.parallaxsports.user.service;

import dev.parallaxsports.core.exception.ResourceNotFoundException;
import dev.parallaxsports.user.dto.UpdateUserRequest;
import dev.parallaxsports.user.dto.UserResponse;
import dev.parallaxsports.user.model.User;
import dev.parallaxsports.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        return toResponse(user);
    }

    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setDisplayName(request.displayName());
        return toResponse(userRepository.save(user));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }
}
