package com.appname.userservice.service.impl;

import com.appname.userservice.dto.request.CreatePaymentCardRequest;
import com.appname.userservice.dto.response.PaymentCardResponse;
import com.appname.userservice.entity.PaymentCard;
import com.appname.userservice.entity.User;
import com.appname.userservice.exception.CardLimitExceededException;
import com.appname.userservice.exception.DuplicateResourceException;
import com.appname.userservice.exception.ResourceNotFoundException;
import com.appname.userservice.mapper.PaymentCardMapper;
import com.appname.userservice.repository.PaymentCardRepository;
import com.appname.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceImplTest {
  @Mock private PaymentCardRepository cardRepository;
  @Mock private UserRepository userRepository;
  @Mock private PaymentCardMapper cardMapper;

  @InjectMocks
  private PaymentCardServiceImpl cardService;

  private User user;
  private PaymentCard card;
  private PaymentCardResponse cardResponse;
  private CreatePaymentCardRequest createRequest;

  @BeforeEach
  void setUp() {
    user = User.builder().id(1L).name("John").surname("Doe").email("john@test.com").active(true).build();

    card = PaymentCard.builder().id(1L).user(user).number("1234567890123456")
            .holder("JOHN DOE").expirationDate(LocalDate.of(2027, 12, 31)).active(true).build();

    cardResponse = new PaymentCardResponse();
    cardResponse.setId(1L);
    cardResponse.setUserId(1L);
    cardResponse.setNumber("1234567890123456");
    cardResponse.setHolder("JOHN DOE");

    createRequest = new CreatePaymentCardRequest();
    createRequest.setNumber("1234567890123456");
    createRequest.setHolder("JOHN DOE");
    createRequest.setExpirationDate(LocalDate.of(2027, 12, 31));
  }

  @Test
  @DisplayName("createCard - success")
  void createCard_Success() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(cardRepository.countCardsByUserId(1L)).thenReturn(0L);
    when(cardRepository.existsByNumber(createRequest.getNumber())).thenReturn(false);
    when(cardMapper.toEntity(createRequest)).thenReturn(card);
    when(cardRepository.save(card)).thenReturn(card);
    when(cardMapper.toResponse(card)).thenReturn(cardResponse);

    PaymentCardResponse result = cardService.createCard(1L, createRequest);

    assertThat(result).isNotNull();
    assertThat(result.getNumber()).isEqualTo("1234567890123456");
    verify(cardRepository).save(card);
  }

  @Test
  @DisplayName("createCard - throws CardLimitExceededException when user has 5 cards")
  void createCard_LimitExceeded_ThrowsException() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(cardRepository.countCardsByUserId(1L)).thenReturn(5L);

    assertThatThrownBy(() -> cardService.createCard(1L, createRequest)).isInstanceOf(CardLimitExceededException.class)
            .hasMessageContaining("1");

    verify(cardRepository, never()).save(any());
  }

  @Test
  @DisplayName("createCard - throws DuplicateResourceException when card number exists")
  void createCard_DuplicateNumber_ThrowsException() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(cardRepository.countCardsByUserId(1L)).thenReturn(2L);
    when(cardRepository.existsByNumber(createRequest.getNumber())).thenReturn(true);

    assertThatThrownBy(() -> cardService.createCard(1L, createRequest)).isInstanceOf(DuplicateResourceException.class);

    verify(cardRepository, never()).save(any());
  }

  @Test
  @DisplayName("createCard - throws ResourceNotFoundException when user not found")
  void createCard_UserNotFound_ThrowsException() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> cardService.createCard(99L, createRequest)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("getCardById - success")
  void getCardById_Success() {
    when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
    when(cardMapper.toResponse(card)).thenReturn(cardResponse);

    PaymentCardResponse result = cardService.getCardById(1L);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("getCardById - throws ResourceNotFoundException when card not found")
  void getCardById_NotFound_ThrowsException() {
    when(cardRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> cardService.getCardById(99L)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("getCardsByUserId - success")
  void getCardsByUserId_Success() {
    when(userRepository.existsById(1L)).thenReturn(true);
    when(cardRepository.findByUserId(1L)).thenReturn(List.of(card));
    when(cardMapper.toResponse(card)).thenReturn(cardResponse);

    List<PaymentCardResponse> result = cardService.getCardsByUserId(1L);

    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("deleteCard - success")
  void deleteCard_Success() {
    when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
    doNothing().when(cardRepository).delete(card);

    assertThatCode(() -> cardService.deleteCard(1L)).doesNotThrowAnyException();
    verify(cardRepository).delete(card);
  }

  @Test
  @DisplayName("activateCard - success")
  void activateCard_Success() {
    when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
    when(cardRepository.updateActiveStatus(1L, true)).thenReturn(1);

    assertThatCode(() -> cardService.activateCard(1L)).doesNotThrowAnyException();
    verify(cardRepository).updateActiveStatus(1L, true);
  }

  @Test
  @DisplayName("deactivateCard - success")
  void deactivateCard_Success() {
    when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
    when(cardRepository.updateActiveStatus(1L, false)).thenReturn(1);

    assertThatCode(() -> cardService.deactivateCard(1L)).doesNotThrowAnyException();
    verify(cardRepository).updateActiveStatus(1L, false);
  }

}