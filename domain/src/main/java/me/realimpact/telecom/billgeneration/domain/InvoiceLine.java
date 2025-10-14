package me.realimpact.telecom.billgeneration.domain;

import lombok.Builder;
import lombok.Value;

/**
 * Minimal invoice line placeholder to allow package-based scaffolding
 * without impacting existing modules.
 */
@Value
@Builder
public class InvoiceLine {
    String description;
    long amount; // smallest currency unit (e.g., KRW)
}
