package org.example.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.adapter.in.web.auth.CurrentPrincipalResolver;
import org.example.application.dto.RegisterUserRequestDto;
import org.example.application.dto.UpdateUserRequestDto;
import org.example.application.dto.UserResponseDto;
import org.example.application.service.UserManagementApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserManagementController {

    private final UserManagementApplicationService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto register(@RequestBody RegisterUserRequestDto request) {
        return userService.register(request);
    }

    @GetMapping("/me")
    public UserResponseDto me(HttpServletRequest request) {
        return userService.find(currentUserId(request));
    }

    @PutMapping("/me")
    public UserResponseDto updateMe(
            HttpServletRequest request,
            @RequestBody UpdateUserRequestDto body
    ) {
        return userService.update(currentUserId(request), body);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(HttpServletRequest request) {
        userService.delete(currentUserId(request));
    }

    private Long currentUserId(HttpServletRequest request) {
        return CurrentPrincipalResolver.resolve(request).id();
    }
}
