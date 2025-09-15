package me.realimpact.telecom.testgen.mapper;

import me.realimpact.telecom.testgen.entity.*;
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
    void truncateDeviceInstallmentDetails();
    void truncateDeviceInstallmentMasters();
    void truncateInstallationHistories();
    
    // 데이터 삽입
    void insertContracts(List<ContractEntity> contracts);
    void insertProducts(List<ProductEntity> products);
    void insertSuspensions(List<SuspensionEntity> suspensions);
    void insertDeviceInstallmentMasters(List<DeviceInstallmentMasterEntity> masters);
    void insertDeviceInstallmentDetails(List<DeviceInstallmentDetailEntity> details);
    void insertInstallationHistories(List<InstallationHistoryEntity> histories);
    
    // 조회
    List<String> selectProductOfferingIds();
    List<Long> selectContractIds();

    // 테이블 레코드 수 확인 (TRUNCATE 검증용)
    int countSuspensions();
    int countProducts();
    int countContracts();
    int countDeviceInstallmentDetails();
    int countDeviceInstallmentMasters();
    int countInstallationHistories();
}