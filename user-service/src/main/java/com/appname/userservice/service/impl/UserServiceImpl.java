package com.appname.userservice.service.impl;

import com.appname.userservice.cache.CacheConstants;
import com.appname.userservice.dto.request.CreateUserRequest;
import com.appname.userservice.dto.request.UpdateUserRequest;
import com.appname.userservice.dto.request.UserFilterRequest;
import com.appname.userservice.dto.response.UserResponse;
import com.appname.userservice.entity.PaymentCard;
import com.appname.userservice.entity.User;
import com.appname.userservice.exception.DuplicateResourceException;
import com.appname.userservice.exception.ResourceNotFoundException;
import com.appname.userservice.mapper.UserMapper;
import com.appname.userservice.repository.UserRepository;
import com.appname.userservice.repository.UserSpecification;
import com.appname.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Override
  @Transactional
  public UserResponse createUser(CreateUserRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new DuplicateResourceException("User with email " + request.getEmail() + " already exists");
    }
    User user = userMapper.toEntity(request);
    User saved = userRepository.save(user);
    log.info("Created user with id: {}", saved.getId());
    return userMapper.toResponse(saved);
  }

  @Override
  @Cacheable(value = CacheConstants.USERS_CACHE, key = "#id")
  public UserResponse getUserById(Long id) {
    log.info("Fetching user with id: {} (cache miss)", id);
    User user = findUserOrThrow(id);
    return userMapper.toResponse(user);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<UserResponse> getAllUsers(UserFilterRequest filter, Pageable pageable) {
    Specification<User> spec = Specification.where(UserSpecification.hasName(filter.getName()))
            .and(UserSpecification.hasSurname(filter.getSurname())).and(UserSpecification.isActive(filter.getActive()));
    return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
  }

  @Override
  @Transactional
  @CacheEvict(value = CacheConstants.USERS_CACHE, key = "#id")
  public UserResponse updateUser(Long id, UpdateUserRequest request) {
    User user = findUserOrThrow(id);
    if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
      if (userRepository.existsByEmail(request.getEmail())) {
        throw new DuplicateResourceException("Email " + request.getEmail() + " is already in use");
      }
    }
    userMapper.updateEntityFromRequest(request, user);
    User updated = userRepository.save(user);
    log.info("Updated user with id: {}", id);
    return userMapper.toResponse(updated);
  }

  @Override
  @Transactional
  @CacheEvict(value = CacheConstants.USERS_CACHE, key = "#id")
  public void activateUser(Long id) {
    findUserOrThrow(id);
    userRepository.updateActiveStatus(id, true);
    log.info("Activated user with id: {}", id);
  }

  @Override
  @Transactional
  @CacheEvict(value = CacheConstants.USERS_CACHE, key = "#id")
  public void deactivateUser(Long id) {
    findUserOrThrow(id);
    userRepository.updateActiveStatus(id, false);
    log.info("Deactivated user with id: {}", id);
  }

  @Override
  @Transactional
  @Caching(evict = {@CacheEvict(value = CacheConstants.USERS_CACHE, key = "#id", beforeInvocation = false),
          @CacheEvict(value = CacheConstants.CARDS_CACHE, allEntries = true, beforeInvocation = false)})
  public UserResponse deleteUser(Long id) {
    User user = findUserOrThrow(id);
    user.setActive(false);
    List<PaymentCard> cards = user.getPaymentCards();
    for (PaymentCard card: cards){
      card.setActive(false);
    }
    user.setPaymentCards(cards);
    User updated = userRepository.save(user);
    log.info("Deleted user with id: {}", id);
    return userMapper.toResponse(updated);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserResponse> getUsersByIds(List<Long> ids) {
    return userRepository.findAllById(ids).stream().map(userMapper::toResponse).toList();
  }

  private User findUserOrThrow(Long id) {
    return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
  }

}
