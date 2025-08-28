package me.realimpact.telecom.billing.batch.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.billing.batch.CalculationParameters;
import me.realimpact.telecom.billing.batch.CalculationResultGroup;
import me.realimpact.telecom.calculation.domain.CalculationResult;
import me.realimpact.telecom.calculation.port.out.CalculationResultSavePort;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

/**
 * MonthlyFeeCalculationResult를 데이터베이스에 저장하는 커스텀 Writer
 */
@RequiredArgsConstructor
@Slf4j
public class CalculationWriter implements ItemWriter<CalculationResultGroup> {

    private final CalculationResultSavePort calculationResultSavePort;
    private final CalculationParameters calculationParameters;

    @Override
    public void write(Chunk<? extends CalculationResultGroup> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }
        List<CalculationResult> calculationResults = chunk.getItems().stream()
            .flatMap(calculationResultGroup -> calculationResultGroup.calculationResults().stream())
            .toList();

        calculationResultSavePort.save(calculationParameters.toCalculationContext(), calculationResults);
        
    }
}