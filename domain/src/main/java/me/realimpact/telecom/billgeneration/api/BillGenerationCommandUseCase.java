package me.realimpact.telecom.billgeneration.api;

import java.util.List;

/**
 * Use case for generating bills. This is intentionally minimal so we can
 * evolve inputs/outputs later without impacting other modules.
 */
public interface BillGenerationCommandUseCase {
    /**
     * Generate bills for the given contract IDs.
     *
     * @param contractIds target contract identifiers
     */
    void generateForContracts(List<Long> contractIds);
}
