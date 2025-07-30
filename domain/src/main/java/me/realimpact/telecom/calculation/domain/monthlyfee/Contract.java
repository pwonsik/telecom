package me.realimpact.telecom.calculation.domain.monthlyfee;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Contract extends Temporal {
    private final Long contractId;
    
    private final LocalDate subscribedAt;
    private final LocalDate initiallySubscribedAt;
    private final Optional<LocalDate> terminatedAt;
    private final Optional<LocalDate> prefferedTerminationDate;


    @Override
    public LocalDate getStartDate() {
        return List.of(
            subscribedAt, 
            initiallySubscribedAt
        ).stream().max(Comparator.naturalOrder()).orElseThrow();
    }

    @Override
    public LocalDate getEndDate() {
        return List.of(
            terminatedAt.orElse(LocalDate.MAX),
            prefferedTerminationDate.orElse(LocalDate.MAX)
        ).stream().min(Comparator.naturalOrder()).orElseThrow();
    }

    public Object getServiceCode() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getServiceCode'");
    }
}
