package me.realimpact.telecom.testgen.mapper;

import me.realimpact.telecom.testgen.entity.ContractEntity;
import me.realimpact.telecom.testgen.entity.ProductEntity;
import me.realimpact.telecom.testgen.entity.SuspensionEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TestDataMapper {
    
    void insertContracts(List<ContractEntity> contracts);
    
    void insertProducts(List<ProductEntity> products);
    
    void insertSuspensions(List<SuspensionEntity> suspensions);
    
    List<String> selectProductOfferingIds();
}