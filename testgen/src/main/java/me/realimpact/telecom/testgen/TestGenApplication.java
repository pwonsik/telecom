package me.realimpact.telecom.testgen;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.realimpact.telecom.testgen.service.TestDataGeneratorService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class TestGenApplication implements CommandLineRunner {

    private final TestDataGeneratorService testDataGeneratorService;

    public static void main(String[] args) {
        SpringApplication.run(TestGenApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            log.error("사용법: java -jar testgen.jar <계약_개수>");
            log.error("예시: java -jar testgen.jar 1000");
            System.exit(1);
            return;
        }

        try {
            int contractCount = Integer.parseInt(args[0]);
            if (contractCount <= 0) {
                log.error("계약 개수는 1 이상이어야 합니다: {}", contractCount);
                System.exit(1);
                return;
            }

            log.info("=== 테스트 데이터 생성기 시작 ===");
            log.info("생성할 계약 개수: {}", contractCount);
            
            long startTime = System.currentTimeMillis();
            testDataGeneratorService.generateTestData(contractCount);
            long endTime = System.currentTimeMillis();
            
            log.info("=== 테스트 데이터 생성 완료 ===");
            log.info("소요 시간: {} ms ({} 초)", endTime - startTime, (endTime - startTime) / 1000.0);
            log.info("초당 처리 건수: {}", Math.round(contractCount * 1000.0 / (endTime - startTime)));
            
        } catch (NumberFormatException e) {
            log.error("잘못된 숫자 형식: {}", args[0]);
            System.exit(1);
        } catch (Exception e) {
            log.error("테스트 데이터 생성 중 오류 발생", e);
            System.exit(1);
        }
    }
}