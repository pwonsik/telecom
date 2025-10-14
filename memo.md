loadMonthlyFeeDataByType, loadOneTimeChargeDataByType 이 타입만 다르고 패턴은 같은데, 이 두개를 합칠수는 없을까?

네, 아주 좋은 관찰입니다. 두 메소드는 제네릭 타입만 다를 뿐, 내부 로직과 패턴이 완전히 동일합니다. 이런 중복은 **제네릭 메소드(Generic Method)**를 활용하여 멋지게 하나로 합칠 수 있습니다.

제가 직접 CalculationCommandService.java 파일을 수정하여 두 메소드를 하나로 통합하고, 기존 코드가 이 새로운 공통 메소드를 호출하도록 리팩토링해 드리겠습니다.

리팩토링 계획
loadDataByType 제네릭 메소드 생성:

loadMonthlyFeeDataByType과 loadOneTimeChargeDataByType의 공통 로직을 추출하여, 어떤 도메인 타입(T)이든 처리할 수 있는 loadDataByType이라는 새로운 private 제네릭 메소드를 만듭니다.
이 메소드는 데이터 로더 맵(Map<Class<? extends T>, ? extends DataLoader<? extends T>>)을 파라미터로 받습니다.
기존 메소드 수정:

loadCalculationTargets 메소드 내부에서 loadMonthlyFeeDataByType과 loadOneTimeChargeDataByType을 호출하던 부분을, 새로 만든 loadDataByType 제네릭 메소드를 호출하도록 변경합니다.
CalculationCommandService.java 파일 수정
아래와 같이 파일을 수정하겠습니다.


// ... 기존 코드 ...
    public List<CalculationTarget> loadCalculationTargets(List<Long> contractIds, CalculationContext ctx) {
        // Monthly Fee 데이터를 Map으로 로딩 (리팩토링된 메소드 호출)
        var monthlyFeeDataByType = loadDataByType(contractIds, ctx, monthlyFeeDataLoaderMap);

        // OneTimeCharge 데이터를 Map으로 로딩 (리팩토링된 메소드 호출)
        var oneTimeChargeDataByType = loadDataByType(contractIds, ctx, oneTimeChargeDataLoaderMap);

        // 할인 (기존 방식 유지)
// ... 기존 코드 ...
    }


    /**
     * 모든 MonthlyFeeDataLoader를 실행하여 데이터 로딩
     * @deprecated loadDataByType 제네릭 메소드로 대체됨
     */
    private Map<Class<? extends MonthlyChargeDomain>, Map<Long, List<? extends MonthlyChargeDomain>>>
        loadMonthlyFeeDataByType(List<Long> contractIds, CalculationContext context) {
// ... 기존 코드 ...
    }

// ... 기존 코드 ...

    /**
     * 모든 DataLoader를 실행하여 OneTimeCharge 데이터 로딩
     * @deprecated loadDataByType 제네릭 메소드로 대체됨
     */
    private Map<Class<? extends OneTimeChargeDomain>, Map<Long, List<? extends OneTimeChargeDomain>>>
        loadOneTimeChargeDataByType(List<Long> contractIds, CalculationContext context) {
// ... 기존 코드 ...
    }

    /**
     * [리팩토링] 데이터 로더 맵을 받아 데이터를 로딩하는 제네릭 메소드
     * @param contractIds 계약 ID 목록
     * @param context 계산 컨텍스트
     * @param dataLoaderMap 처리할 데이터 로더 맵
     * @param <T> 도메인 타입 (예: MonthlyChargeDomain, OneTimeChargeDomain)
     * @param <L> 데이터 로더 타입
     * @return 로딩된 데이터를 담은 맵
     */
    private <T, L extends DataLoader<T>> Map<Class<? extends T>, Map<Long, List<? extends T>>>
        loadDataByType(List<Long> contractIds, CalculationContext context, Map<Class<? extends T>, L> dataLoaderMap) {

        Map<Class<? extends T>, Map<Long, List<? extends T>>> result = new HashMap<>();

        for (var entry : dataLoaderMap.entrySet()) {
            var dataType = entry.getKey();
            var loader = entry.getValue();
            Map<Long, List<? extends T>> data = loader.read(contractIds, context);
            if (data != null && !data.isEmpty()) {
                result.put(dataType, data);
            }
        }
        return result;
    }

// ... 기존 코드 ...