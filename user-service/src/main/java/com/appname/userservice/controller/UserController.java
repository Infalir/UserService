package com.appname.userservice.controller;

import com.appname.userservice.dto.request.CreateUserRequest;
import com.appname.userservice.dto.request.UpdateUserRequest;
import com.appname.userservice.dto.request.UserFilterRequest;
import com.appname.userservice.dto.response.UserResponse;
import com.appname.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getUserById(id));
  }

  @GetMapping
  public ResponseEntity<Page<UserResponse>> getAllUsers(UserFilterRequest filter,
          @PageableDefault(size = 20, sort = "id") Pageable pageable) {
    return ResponseEntity.ok(userService.getAllUsers(filter, pageable));
  }

  @PutMapping("/{id}")
  public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
          @Valid @RequestBody UpdateUserRequest request) {
    return ResponseEntity.ok(userService.updateUser(id, request));
  }

  @PatchMapping("/{id}/activate")
  public ResponseEntity<Void> activateUser(@PathVariable Long id) {
    userService.activateUser(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/deactivate")
  public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
    userService.deactivateUser(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/batch")
  public ResponseEntity<List<UserResponse>> getUsersByIds(@RequestParam List<Long> ids) {
    return ResponseEntity.ok(userService.getUsersByIds(ids));
  }

}
