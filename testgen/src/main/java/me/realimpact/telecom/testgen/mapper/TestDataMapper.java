package me.realimpact.telecom.testgen.mapper;

import me.realimpact.telecom.testgen.entity.ContractEntity;
import me.realimpact.telecom.testgen.entity.ProductEntity;
import me.realimpact.telecom.testgen.entity.SuspensionEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TestDataMapper {
    
    // 테이블 초기화 (외래키 제약조건 해결)
    void disableForeignKeyChecks();
    void enableForeignKeyChecks();
    void truncateSuspensions();
    void truncateProducts();  
    void truncateContracts();
    
    // 데이터 삽입
    void insertContracts(List<ContractEntity> contracts);
    void insertProducts(List<ProductEntity> products);
    void insertSuspensions(List<SuspensionEntity> suspensions);
    
    // 조회
    List<String> selectProductOfferingIds();
}