package me.realimpact.telecom.calculation.domain.monthlyfee;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class AdditionalBillingFactor extends Temporal {
    /*
     * key : ContractAmount, LineCount, LineSpeed.
     * value : 10000, 3, H8, etc.
     */
    private final Map<String, String> factors;

    private final LocalDate effectiveStartDate;
    private final LocalDate effectiveEndDate; 

    @Override
    public LocalDate getStartDate() {
        return effectiveStartDate;
    }

    @Override
    public LocalDate getEndDate() {
        return effectiveEndDate;
    }

    /**
     * 추가 과금 요소 값을 타입에 맞게 반환합니다.
     * 
     * @param key   조회할 요소의 키
     * @param clazz 반환받고자 하는 타입 (예: String.class, Long.class)
     * @return      해당 타입의 Optional 값
     */
    public <T> Optional<T> getFactorValue(String key, Class<T> clazz) {
        if (factors.containsKey(key)) {
            String factorValue = factors.get(key);
            try {
                if (clazz == String.class) {
                    return Optional.of(clazz.cast(factorValue));
                } else if (clazz == Long.class) {
                    return Optional.of(clazz.cast(Long.valueOf(factorValue)));
                }
            } catch (Exception e) {
                throw e;
            }            
        } 
        return Optional.empty();
    }
}