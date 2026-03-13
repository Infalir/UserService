package com.appname.userservice.controller;

import com.appname.userservice.dto.request.CardFilterRequest;
import com.appname.userservice.dto.request.CreatePaymentCardRequest;
import com.appname.userservice.dto.request.UpdatePaymentCardRequest;
import com.appname.userservice.dto.response.PaymentCardResponse;
import com.appname.userservice.service.PaymentCardService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentCardController {
  private final PaymentCardService cardService;

  @PostMapping("/users/{userId}/cards")
  public ResponseEntity<PaymentCardResponse> createCard(@PathVariable Long userId,
          @Valid @RequestBody CreatePaymentCardRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(userId, request));
  }

  @GetMapping("/cards/{id}")
  public ResponseEntity<PaymentCardResponse> getCardById(@PathVariable Long id) {
    return ResponseEntity.ok(cardService.getCardById(id));
  }

  @GetMapping("/cards")
  public ResponseEntity<Page<PaymentCardResponse>> getAllCards(CardFilterRequest filter,
          @PageableDefault(size = 20, sort = "id") Pageable pageable) {
    return ResponseEntity.ok(cardService.getAllCards(filter, pageable));
  }

  @GetMapping("/users/{userId}/cards")
  public ResponseEntity<List<PaymentCardResponse>> getCardsByUserId(@PathVariable Long userId) {
    return ResponseEntity.ok(cardService.getCardsByUserId(userId));
  }

  @PutMapping("/cards/{id}")
  public ResponseEntity<PaymentCardResponse> updateCard(@PathVariable Long id,
          @Valid @RequestBody UpdatePaymentCardRequest request) {
    return ResponseEntity.ok(cardService.updateCard(id, request));
  }

  @PatchMapping("/cards/{id}/activate")
  public ResponseEntity<Void> activateCard(@PathVariable Long id) {
    cardService.activateCard(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/cards/{id}/deactivate")
  public ResponseEntity<Void> deactivateCard(@PathVariable Long id) {
    cardService.deactivateCard(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/cards/{id}")
  public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
    cardService.deleteCard(id);
    return ResponseEntity.noContent().build();
  }

}
