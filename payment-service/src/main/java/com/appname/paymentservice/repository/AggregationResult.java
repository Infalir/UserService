package com.appname.paymentservice.repository;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.Decimal128;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class AggregationResult {
  private Decimal128 total;
}
