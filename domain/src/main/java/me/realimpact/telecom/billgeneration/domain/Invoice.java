package me.realimpact.telecom.billgeneration.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

/**
 * Minimal placeholder aggregate for bill generation domain.
 */
@Value
@Builder
public class Invoice {
    Long contractId;
    LocalDate billDate;
    List<InvoiceLine> lines;
    long totalAmount;
}
