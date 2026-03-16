package com.appname.userservice.service.impl;

import com.appname.userservice.dto.request.CreateUserRequest;
import com.appname.userservice.dto.request.UpdateUserRequest;
import com.appname.userservice.dto.request.UserFilterRequest;
import com.appname.userservice.dto.response.UserResponse;
import com.appname.userservice.entity.User;
import com.appname.userservice.exception.DuplicateResourceException;
import com.appname.userservice.exception.ResourceNotFoundException;
import com.appname.userservice.mapper.UserMapper;
import com.appname.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @InjectMocks
  private UserServiceImpl userService;

  private User user;
  private UserResponse userResponse;
  private UserResponse inactiveUserResponse;
  private CreateUserRequest createRequest;
  private UpdateUserRequest updateRequest;

  @BeforeEach
  void setUp() {
    user = User.builder().id(1L).name("John").surname("Doe").email("john.doe@example.com")
            .birthDate(LocalDate.of(1990, 1, 1)).active(true).build();

    userResponse = new UserResponse();
    userResponse.setId(1L);
    userResponse.setName("John");
    userResponse.setSurname("Doe");
    userResponse.setEmail("john.doe@example.com");
    userResponse.setActive(true);

    inactiveUserResponse = new UserResponse();
    inactiveUserResponse.setId(1L);
    inactiveUserResponse.setActive(false);

    createRequest = new CreateUserRequest();
    createRequest.setName("John");
    createRequest.setSurname("Doe");
    createRequest.setEmail("john.doe@example.com");
    createRequest.setBirthDate(LocalDate.of(1990, 1, 1));

    updateRequest = new UpdateUserRequest();
    updateRequest.setName("Jane");
  }

  @Test
  @DisplayName("createUser - success")
  void createUser_Success() {
    when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
    when(userMapper.toEntity(createRequest)).thenReturn(user);
    when(userRepository.save(user)).thenReturn(user);
    when(userMapper.toResponse(user)).thenReturn(userResponse);

    UserResponse result = userService.createUser(createRequest);

    assertThat(result).isNotNull();
    assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
    verify(userRepository).save(user);
  }

  @Test
  @DisplayName("createUser - throws DuplicateResourceException when email already exists")
  void createUser_DuplicateEmail_ThrowsException() {
    when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(true);

    assertThatThrownBy(() -> userService.createUser(createRequest))
            .isInstanceOf(DuplicateResourceException.class).hasMessageContaining(createRequest.getEmail());

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("getUserById - success")
  void getUserById_Success() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userMapper.toResponse(user)).thenReturn(userResponse);

    UserResponse result = userService.getUserById(1L);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("getUserById - throws ResourceNotFoundException when user not found")
  void getUserById_NotFound_ThrowsException() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUserById(99L)).isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
  }

  @Test
  @DisplayName("getAllUsers - returns paginated results")
  void getAllUsers_ReturnsPage() {
    UserFilterRequest filter = new UserFilterRequest();
    Pageable pageable = PageRequest.of(0, 10);
    Page<User> userPage = new PageImpl<>(List.of(user));

    when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
    when(userMapper.toResponse(user)).thenReturn(userResponse);

    Page<UserResponse> result = userService.getAllUsers(filter, pageable);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
  }

  @Test
  @DisplayName("updateUser - success")
  void updateUser_Success() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(user);
    when(userMapper.toResponse(user)).thenReturn(userResponse);

    UserResponse result = userService.updateUser(1L, updateRequest);

    assertThat(result).isNotNull();
    verify(userMapper).updateEntityFromRequest(updateRequest, user);
    verify(userRepository).save(user);
  }

  @Test
  @DisplayName("updateUser - throws DuplicateResourceException when new email already in use")
  void updateUser_DuplicateEmail_ThrowsException() {
    updateRequest.setEmail("taken@example.com");

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

    assertThatThrownBy(() -> userService.updateUser(1L, updateRequest)).isInstanceOf(DuplicateResourceException.class);

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("activateUser - success")
  void activateUser_Success() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.updateActiveStatus(1L, true)).thenReturn(1);

    assertThatCode(() -> userService.activateUser(1L)).doesNotThrowAnyException();
    verify(userRepository).updateActiveStatus(1L, true);
  }

  @Test
  @DisplayName("deactivateUser - success")
  void deactivateUser_Success() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.updateActiveStatus(1L, false)).thenReturn(1);

    assertThatCode(() -> userService.deactivateUser(1L)).doesNotThrowAnyException();
    verify(userRepository).updateActiveStatus(1L, false);
  }

  @Test
  @DisplayName("deleteUser - success")
  void deleteUser_Success() {
    User softDeletedUser = User.builder().id(1L).active(false).build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(softDeletedUser);
    when(userMapper.toResponse(softDeletedUser)).thenReturn(inactiveUserResponse);

    UserResponse result = userService.deleteUser(1L);

    assertThat(result).isNotNull();
    assertThat(result.getActive()).isFalse();
    verify(userRepository, never()).deleteById(any());
    verify(userRepository).save(user);
  }

  @Test
  @DisplayName("deleteUser - throws ResourceNotFoundException when user not found")
  void deleteUser_NotFound_ThrowsException() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.deleteUser(99L))
            .isInstanceOf(ResourceNotFoundException.class);

    verify(userRepository, never()).deleteById(any());
    verify(userRepository, never()).save(any());
  }

}