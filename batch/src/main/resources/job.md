## 작업실행
.\gradlew :batch:bootRun --args="--spring.batch.job.names=partitionedMonthlyFeeCalculationJob --billingStartDate=2025-10-01 --billingEndDate=2025-10-31 --billingCalculationType=B0 --billingCalculationPeriod=0"



## 작업번호로 파티션 키 구성하기
8 /**
   19  * Job 파라미터로 전달된 '작업번호' 목록을 지정된 gridSize(스레드 수)만큼의 파티션으로 분배하는 Partitioner.
   20  */
   21 @Slf4j
   22 @Component
   23 @Scope(value = "step", proxyMode = ScopedProxyMode.INTERFACES)
   24 public class ContractPartitioner implements Partitioner {
   25
   26     private final List<String> workNumbers;
   27
   28     // SpEL을 사용하여 주입 시점에 문자열을 List로 변환
   29     public ContractPartitioner(@Value("#{(jobParameters['workNumbers'] ?: '').split(',')}") List<String> workNumbers) {
   30         if (CollectionUtils.isEmpty(workNumbers) || workNumbers.get(0).isEmpty()) {
   31             throw new IllegalArgumentException("Job 파라미터 'workNumbers'가 비어있습니다. (예: workNumbers=1001,1002,...)");
   32         }
   33         this.workNumbers = workNumbers;
   34     }
   35
   36     /**
   37      * 전체 작업번호 목록을 gridSize 개수의 파티션으로 분할한다.
   38      * @param gridSize 파티션의 수 (스레드 수와 동일)
   39      * @return 파티션 이름과 ExecutionContext를 담은 맵
   40      */
   41     @Override
   42     public Map<String, ExecutionContext> partition(int gridSize) {
   43         Map<String, ExecutionContext> partitions = new HashMap<>();
   44         int totalWorkCount = workNumbers.size();
   45         int partitionSize = (int) Math.ceil((double) totalWorkCount / gridSize);
   46
   47         log.info("=== 파티션 생성 시작 ===");
   48         log.info("총 작업번호 개수: {}", totalWorkCount);
   49         log.info("요청된 파티션 수 (gridSize): {}", gridSize);
   50         log.info("각 파티션에 할당될 작업 개수 (계산값): {}", partitionSize);
   51
   52         List<List<String>> subLists = IntStream.range(0, gridSize)
   53                 .mapToObj(i -> workNumbers.subList(
   54                         Math.min(i * partitionSize, totalWorkCount),
   55                         Math.min((i + 1) * partitionSize, totalWorkCount)
   56                 ))
   57                 .filter(sublist -> !sublist.isEmpty())
   58                 .collect(Collectors.toList());
   59
   60         for (int i = 0; i < subLists.size(); i++) {
   61             ExecutionContext context = new ExecutionContext();
   62             List<String> assignedWork = subLists.get(i);
   63
   64             String assignedWorkCsv = String.join(",", assignedWork);
   65             context.putString("assignedWorkNumbers", assignedWorkCsv);
   66
   67             String partitionName = "partition" + i;
   68             partitions.put(partitionName, context);
   69
   70             log.info("파티션 생성: {} (할당된 작업번호 {}개)", partitionName, assignedWork.size());
   71         }
   72
   73         log.info("총 {} 개 파티션 생성 완료", partitions.size());
   74         return partitions;
   75     }
   76 }

  달라진 점:

   - `@Value("#{(jobParameters['workNumbers'] ?: '').split(',')}") List<String> workNumbers`
       - jobParameters['workNumbers']: Job 파라미터에서 workNumbers 값을 가져옵니다.
       - ?: '': workNumbers 파라미터가 없을 경우 null 대신 빈 문자열('')을 사용하도록 하여 NullPointerException을 방지합니다. (Elvis 연산자)
       - .split(','): 가져온 문자열을 쉼표(,) 기준으로 잘라 배열로 만듭니다.
       - #{...}: 전체를 SpEL 표현식으로 감싸줍니다.
       - List<String> workNumbers: Spring이 SpEL의 결과(배열)를 List<String>으로 자동 변환하여 workNumbers 파라미터에 주입해 줍니다.
   - 생성자 내부 로직 변경:
       - 문자열을 직접 다루는 split 코드가 사라지고, 주입받은 List가 비어있는지만 확인하면 되므로 코드가 더 간결해졌습니다.

  이 방법이 훨씬 더 깔끔하고 Spring의 기능을 잘 활용하는 접근 방식입니다.
