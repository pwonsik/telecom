package me.realimpact.telecom.billing.batch.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.calculation.infrastructure.adapter.mybatis.CalculationResultMapper;
import me.realimpact.telecom.calculation.infrastructure.converter.CalculationResultFlattener;
import me.realimpact.telecom.calculation.infrastructure.dto.FlatCalculationResultDto;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

/**
 * MonthlyFeeCalculationResult를 데이터베이스에 저장하는 커스텀 Writer
 */
@RequiredArgsConstructor
@Slf4j
public class MonthlyFeeCalculationResultWriter implements ItemWriter<CalculationResult> {

    private final CalculationResultMapper calculationResultMapper;
    private final CalculationResultFlattener calculationResultFlattener;

    @Override
    public void write(Chunk<? extends CalculationResult> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }

        //log.info("Writing {} calculation results to database", chunk.size());
        
        // MonthlyFeeCalculationResult를 평면화된 DTO로 변환
        List<FlatCalculationResultDto> flatResults = calculationResultFlattener.flattenResults(chunk.getItems());
        
        //log.info("Flattened {} results into {} records for batch insert", chunk.size(), flatResults.size());
        
        // 단일 배치 Insert 실행 - 스프링이 트랜잭션을 관리
        int insertedRows = calculationResultMapper.batchInsertCalculationResults(flatResults);
        
        log.info("Successfully inserted {} records", insertedRows);
    }
}