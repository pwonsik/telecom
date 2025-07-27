package me.realimpact.telecom.calculation.port.out;

import java.util.List;

import me.realimpact.telecom.calculation.domain.monthlyfee.Product;

public interface ProductQueryPort {
    List<Product> findByContractId(Long contractId);
}
