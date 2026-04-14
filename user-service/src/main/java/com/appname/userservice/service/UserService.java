package com.appname.userservice.service;

import com.appname.userservice.dto.request.CreateUserRequest;
import com.appname.userservice.dto.request.UpdateUserRequest;
import com.appname.userservice.dto.request.UserFilterRequest;
import com.appname.userservice.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
  UserResponse createUser(CreateUserRequest request);
  UserResponse getUserById(Long id);
  Page<UserResponse> getAllUsers(UserFilterRequest filter, Pageable pageable);
  UserResponse updateUser(Long id, UpdateUserRequest request);
  void activateUser(Long id);
  void deactivateUser(Long id);
  UserResponse deleteUser(Long id);
  List<UserResponse> getUsersByIds(List<Long> ids);

}
