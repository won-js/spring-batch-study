# 8주차 SPRING BATCH STUDY
- CompositeItemProcessor 으로 여러 단계에 걸쳐 데이터 Transform 하기

## CompositeItemProcessor

---
- CompositeItemProcessor는 Spring Batch에서 제공하는 ItemProcessor 인터페이스를 구현하는 클래스
- 여러 개의 ItemProcessor를 하나의 Processor로 연결하여 여러 단계의 처리를 수행할 수 있도록 함

### 주요 구성 요소
- Delegates: 처리를 수행할 ItemProcessor 목록
- TransactionAttribute: 트랜잭션 속성을 설정

### 장점
- 단계별 처리: 여러 단계로 나누어 처리르 수행하여 코드를 명확하고 이해하기 쉽게 만들 수 있음
- 재사용 가능성: 각 단계별 Processor를 재사용하여 다른 Job에서도 활용 가능
- 유연성: 다양한 ItemProcessor를 조합하여 원하는 처리 과정을 구현할 수 있음

### 단점
- 설정 복잡성: 여러 개의 Processor를 설정하고 관리해야 하기 때문에 설정이 복잡해질 수 있음
- 성능 저하: 여러 단계의 처리 과정을 거치므로 성능이 저하될 수 있음

### 샘플 코드
- 샘플코드는 week7에서 사용한 Mybatis reader 를 그대로 사용

### LowerCaseItemProcessor 작성
```java
/**
 * 이름을 소문자로 변경하는 ItemProcessor
 */
public class LowerCaseItemProcessor implements ItemProcessor<Customer, Customer> {
    @Override
    public Customer process(Customer customer) {
        customer.nameToLowerCase();
        return customer;
    }
}
```
- ItemProcessor를 구현하고, process 메소드를 구현
- 이름을 소문자로 변경

### After20YearsItemProcessor 작성
- 나이에 20년을 더하기 위한 ItemProcessor를 작성
```java
/**
 * 나이에 20년을 더하는 ItemProcessor
 */
public class After20YearsItemProcessor implements ItemProcessor<Customer, Customer> {
    @Override
    public Customer process(Customer customer) throws Exception {
        customer.after20Years();
        return customer;
    }
}
```
- 위와 동일하게 ItemProcessor를 구현하고, process 메소드를 구현
- 단순하게 나이를 20년 더함

### CompositeItemProcessor 작성
```java
    @Bean
    public CompositeItemProcessor<Customer, Customer> compositeItemProcessor () {
        return new CompositeItemProcessorBuilder<Customer, Customer>()
                .delegates(List.of(
                        new LowerCaseItemProcessor(),
                        new After20YearsItemProcessor()
                ))
                .build();
    }
```
- 이제 CompositeItemProcessorBuilder를 이용하여 delegates를 통해서 ItemProcessor가 수행할 순서대로 배열을 만들어 전달

### 전체 샘플코드
```java
@Log
@Configuration
public class CustomCompositeConfiguration {
    private static final int CHUNK_SIZE = 10;

    @Autowired
    MyBatisPagingItemReader myBatisPagingItemReader;

    @Bean
    public CompositeItemProcessor<Customer, Customer> compositeItemProcessor () {
        return new CompositeItemProcessorBuilder<Customer, Customer>()
                .delegates(List.of(
                        new LowerCaseItemProcessor(),
                        new After20YearsItemProcessor()
                ))
                .build();
    }

    @Bean
    public Step compositeStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        log.info("------------------ Init customerJdbcCursorStep -----------------");

        return new StepBuilder("compositeStep", jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(myBatisPagingItemReader)
                .processor(compositeItemProcessor())
                .writer(items -> items.forEach(System.out::println))
                .build();
    }

    @Bean
    public Job compositeJob(Step compositeStep, JobRepository jobRepository) {
        log.info("------------------ Init compositeJob -----------------");
        return new JobBuilder("compositeJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(compositeStep)
                .build();
    }
}
```
### 결과
```
Customer(id=1, name=alice, age=66, grade=D)
Customer(id=2, name=bob, age=71, grade=C)
Customer(id=3, name=charlie, age=63, grade=D)
Customer(id=4, name=diana, age=68, grade=D)
Customer(id=5, name=eve, age=70, grade=D)
Customer(id=6, name=frank, age=76, grade=C)
Customer(id=7, name=grace, age=65, grade=D)
Customer(id=8, name=hank, age=69, grade=D)
Customer(id=9, name=ivy, age=67, grade=D)
Customer(id=10, name=jack, age=72, grade=C)
Customer(id=11, name=kate, age=70, grade=D)
Customer(id=12, name=leo, age=80, grade=C)
Customer(id=13, name=mia, age=68, grade=D)
Customer(id=14, name=nate, age=76, grade=D)
Customer(id=15, name=olivia, age=74, grade=D)
Customer(id=16, name=paul, age=81, grade=C)
Customer(id=17, name=quinn, age=72, grade=D)
Customer(id=18, name=rachel, age=75, grade=D)
Customer(id=19, name=sam, age=79, grade=C)
Customer(id=20, name=tina, age=73, grade=D)
```

### Wrap Up

---
- 마찬가지로 Reader가 청크 만큼 실행되고 processor가 청크만큼 실행되고 Writer가 실행되는 것을 확인

### Ref

---
- https://devocean.sk.com/blog/techBoardDetail.do?ID=166950