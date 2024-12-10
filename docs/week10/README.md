# 10주차 SPRING BATCH STUDY
- 스프링배치 플로우 컨트롤 하기

## SpringBatch Flow Controller 개요

---
- SpringBatch에서 배치 수행 Flow 컨트롤은 여러 Step을 정의하고 조건에 따라 순서대로 실행하거나 특정 Step을 건너 뛸 수 있도록 하는 기능
- FlowBuilder API를 사용하여 설정

## Flow 컨트롤 방법
- next: 현재 Step이 성공적으로 종료되면 다음 Step으로 이동
- from: 특정 Step에서 현재 Step으로 이동
- on: 특정 ExitStatus에 따라 다음 Step을 결정
- to: 특정 Step으로 이동
- stop: 현재 Flow 종료
- end: FlowBuilder 종료

## Flow 컨트롤 샘플 코드

---
### next
- next는 Start 스텝을 수행하고 난 뒤, next 스텝으로 이동하게 된다.
- next는 계속해서 추가될 수 있으며, start -> next -> next ... 순으로 진행되도록 한다.
```java
@Bean
public Job job() {

    return jobBuilderFactory.get("job")
        .start(step1())
        .next(step2())
        .end()
        .build();
}

@Bean
public Step step1() {

    return stepBuilderFactory.get("step1")
        .tasklet(new Tasklet() {

            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

                // ...

                return RepeatStatus.FINISHED;
            }
        })
        .build();
}

@Bean
public Step step2() {

    return stepBuilderFactory.get("step2")
        .tasklet(new Tasklet() {

            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

                // ...

                return RepeatStatus.FINISHED;
            }
        })
        .build();
}
```
### next 전체 샘플
```java
@Slf4j
@Configuration
public class NextStepTaskConfiguration {
    private static final String NEXT_STEP_TASK = "NEXT_STEP_TASK";

    @Autowired
    PlatformTransactionManager platformTransactionManager;

    @Bean(name="step01")
    public Step step01(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("-------------------------- Init myStep --------------------------");

        return new StepBuilder("step01", jobRepository)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                        log.info("Execute Step01 Tasklet ...");
                        return RepeatStatus.FINISHED;
                    }
                }, transactionManager)
                .build();
    }

    @Bean(name = "step02")
    public Step step02(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("------------------ Init myStep -----------------");

        return new StepBuilder("step02", jobRepository)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                        log.info("Execute Step 02 Tasklet ...");
                        return RepeatStatus.FINISHED;
                    }
                }, transactionManager)
                .build();
    }

    @Bean
    public Job nextStepJob(Step step01, Step step02, JobRepository jobRepository) {
        log.info("------------------ Init myJob -----------------");
        return new JobBuilder(NEXT_STEP_TASK, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step01)
                .next(step02)
                .build();
    }
}
```


### on
- on은 특정 스탭의 종료 조건에 따라 어떠한 스탭으로 이동할지 결정할 수 있도록 한다.
- 아래 예는 Step01을 먼저 수행하고, 해당 결과에 따라서 다음 스텝으로 이동하는 플로우를 보여줌
  - on("FAILED")인 경우 step03 을 수행하도록 한다.
  - from(step01).on("COMPLETED")인 경우 step01의 결과 완료인 경우라면 step02를 수행하도록 한다.
- on과 form을 통해서 스탭의 종료 조건에 따라 원하는 플로우를 처리할 수 있다.
```java
@Bean
public Job job() {

    return jobBuilderFactory.get("job")
        .start(step1())
        .on("FAILED").to(step3())
        .from(step1()).on("COMPLETED").to(step2())
        .end()
        .build();
}

@Bean
public Step step1() {

    return stepBuilderFactory.get("step1")
        .tasklet(new Tasklet() {

            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

                // ...

                if (someCondition) {
                    return RepeatStatus.FINISHED;
                } else {
                    throw new RuntimeException();
                }
            }
        })
        .build();
}

@Bean
public Step step2() {

    return stepBuilderFactory.get("step2")
        .tasklet(new Tasklet() {

            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

                // ...

                return RepeatStatus.FINISHED;
            }
        })
        .build();
}

@Bean
public Step step3() {

    return stepBuilderFactory.get("step3")
        .tasklet(new Tasklet() {

            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

                // ...

                return RepeatStatus.FINISHED;
            }
        })
        .build();
}
```

### on 전체 샘플 코드
```java
@Slf4j
@Configuration
public class OnStepTaskJobConfiguration {
    public static final String ON_STEP_TASK = "ON_STEP_TASK";

    @Autowired
    PlatformTransactionManager transactionManager;

    @Bean(name = "stepOn01")
    public Step stepOn01(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("------------------ Init myStep -----------------");

        return new StepBuilder("stepOn01", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("Execute Step 01 Tasklet ...");

                    Random random = new Random();
                    int randomValue = random.nextInt(1000);

                    if (randomValue % 2 == 0) {
                        return RepeatStatus.FINISHED;
                    } else {
                        throw new RuntimeException("Error This value is Odd: " + randomValue);
                    }
                }, transactionManager).build();
    }

    @Bean(name = "stepOn02")
    public Step stepOn02(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("------------------ Init myStep -----------------");

        return new StepBuilder("stepOn02", jobRepository)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                        log.info("Execute Step 02 Tasklet ...");
                        return RepeatStatus.FINISHED;
                    }
                }, transactionManager)
                .build();
    }


    @Bean(name = "stepOn03")
    public Step stepOn03(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("------------------ Init myStep -----------------");

        return new StepBuilder("stepOn03", jobRepository)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                        log.info("Execute Step 03 Tasklet ...");
                        return RepeatStatus.FINISHED;
                    }
                }, transactionManager)
                .build();
    }

    @Bean
    public Job onStepJob(Step stepOn01, Step stepOn02, Step stepOn03, JobRepository jobRepository) {
        log.info("------------------ Init myJob -----------------");
        return new JobBuilder(ON_STEP_TASK, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(stepOn01)
                .on("FAILED").to(stepOn03)
                .from(stepOn01).on("COMPLETED").to(stepOn02)
                .end()
                .build();
    }

}
```

### Stop
- stop은 특정 step의 작업 결과의 상태를 보고 정지할지 결정
- 아래 예제는 step01의 결과가 실패인 경우 stop을 통해 배치 작업을 정지하고 있음
```java
@Bean
public Job job() {

    return jobBuilderFactory.get("job")
        .start(step1())
        .on("FAILED").stop()
        .end()
        .build();
}

@Bean
public Step step1() {

    return stepBuilderFactory.get("step1")
        .tasklet(new Tasklet() {

            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

                // ...

                throw new RuntimeException();
            }
        })
        .build();
}
```

### Stop 전체 샘플 코드
```java
@Slf4j
@Configuration
public class StopStepTaskJobConfiguration {
    public static final String STOP_STEP_TASK = "STOP_STEP_TASK";

    @Autowired
    PlatformTransactionManager transactionManager;

    @Bean(name = "stepStop01")
    public Step stepStop01(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("------------------ Init myStep -----------------");

        return new StepBuilder("stepStop01", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("Execute Step 01 Tasklet ...");

                    Random random = new Random();
                    int randomValue = random.nextInt(1000);

                    if (randomValue % 2 == 0) {
                        return RepeatStatus.FINISHED;
                    } else {
                        throw new RuntimeException("Error This value is Odd: " + randomValue);
                    }
                }, transactionManager)
                .build();
    }

    @Bean(name = "stepStop02")
    public Step stepStop02(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("------------------ Init myStep -----------------");

        return new StepBuilder("stepStop02", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("Execute Step 02 Tasklet ...");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Job stopStepJob(Step stepOn01, Step stepOn02, Step stepOn03, JobRepository jobRepository) {
        log.info("------------------ Init myJob -----------------");
        return new JobBuilder(STOP_STEP_TASK, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(stepOn01)
                .on("FAILED").stop()
                .from(stepOn01).on("COMPLETED").to(stepOn02)
                .end()
                .build();
    }
}
```

## Wrap up

---
- start, next를 통해서 배치 job의 여러 step을 이어서 수행 가능
- on과 from을 통해서 특정 조건에 따라 스텝을 분기처리 할 수 있음
- stop을 통해서 특정 조건이 되면 배치 작업 종료 가능

### ref - https://devocean.sk.com/blog/techBoardDetail.do?ID=167054