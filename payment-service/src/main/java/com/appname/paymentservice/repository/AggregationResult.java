package com.appname.paymentservice.repository;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.Decimal128;

import java.math.BigDecimal;

/**
 * Wrapper DTO for MongoDB aggregation pipeline results that compute a sum.
 *
 * <p>Spring Data MongoDB cannot deserialize aggregation output directly into
 * a raw {@link BigDecimal} return type due to Java module system restrictions
 * ({@code java.math} is not open to unnamed modules). This wrapper class holds
 * the {@code total} field that the {@code $group: { total: { $sum: ... } }}
 * pipeline stage produces, and Spring Data can deserialize it correctly.</p>
 */
@Data
@NoArgsConstructor
public class AggregationResult {
  private Decimal128 total;
}
