package com.appname.userservice.service;

import com.appname.userservice.dto.request.CardFilterRequest;
import com.appname.userservice.dto.request.CreatePaymentCardRequest;
import com.appname.userservice.dto.request.UpdatePaymentCardRequest;
import com.appname.userservice.dto.response.PaymentCardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentCardService {
  PaymentCardResponse createCard(Long userId, CreatePaymentCardRequest request);
  PaymentCardResponse getCardById(Long id);
  Page<PaymentCardResponse> getAllCards(CardFilterRequest filter, Pageable pageable);
  List<PaymentCardResponse> getCardsByUserId(Long userId);
  PaymentCardResponse updateCard(Long id, UpdatePaymentCardRequest request);
  void activateCard(Long id);
  void deactivateCard(Long id);
  PaymentCardResponse deleteCard(Long id);

}
