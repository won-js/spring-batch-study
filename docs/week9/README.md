# 9주차 SPRING BATCH STUDY
- 입맛에 맞는 배치 처리를 위한 Custom ItemReader / ItemWriter 구현 방법 알아보기

ItemReader: QueryDsl을 이용하여 paging하여 데이터베이스의 값을 읽어 들이는 QuerydslPagingItemReader 구현해보기
ItemWriter: CustomItemWriter를 구현하여 타 서비스를 호출하는 샘플 만들어보기

## QuerydslPagingItemReader 개요

---
- Querydsl은 SpringBatch의 공식 ItemReader가 아니다.
- AbstractPagingItemReader를 이용하여 Qeurydsl를 활용할 수 있도록 ItemReader를 만들어 볼 것
- Querydsl 기능 활용: Querydsl의 강력하고 유연한 쿼리 기능을 사용하여 데이터를 효율적으로 읽을 수 있다.
- JPA 엔티티 추상화: JPA 엔티티에 직접 의존하지 않고 추상화된 쿼리를 작성하여 코드 유지 관리성을 높일 수 있다.
- 동적 쿼리 지원: 런타임 시 조건에 따라 동적으로 쿼리를 생성할 수 있다.

## QuerydslPagingItemReader 생성하기

---
```java
public class QuerydslPagingItemReader<T> extends AbstractPagingItemReader<T> {
    private final EntityManager em;
    private final Function<JPAQueryFactory, JPAQuery<T>> querySupplier;

    private final boolean alwaysReadFromZero;

    public QuerydslPagingItemReader(EntityManagerFactory emf, Function<JPAQueryFactory, JPAQuery<T>> querySupplier, int chunkSize) {
        this(ClassUtils.getShortName(QuerydslPagingItemReader.class), emf, querySupplier, chunkSize, false);
    }

    public QuerydslPagingItemReader(String name, EntityManagerFactory entityManagerFactory, Function<JPAQueryFactory, JPAQuery<T>> querySupplier, int chunkSize, Boolean alwaysReadFromZero) {
        super.setPageSize(chunkSize);
        setName(name);
        this.querySupplier = querySupplier;
        this.em = entityManagerFactory.createEntityManager();
        this.alwaysReadFromZero = alwaysReadFromZero;

    }

    @Override
    protected void doClose() throws Exception {
        if (em != null)
            em.close();
        super.doClose();
    }

    @Override
    protected void doReadPage() {
        initQueryResult();

        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);
        long offset = 0;
        if (!alwaysReadFromZero) {
            offset = (long) getPage() * getPageSize();
        }

        JPAQuery<T> query = querySupplier.apply(jpaQueryFactory).offset(offset).limit(getPageSize());

        List<T> queryResult = query.fetch();
        for (T entity: queryResult) {
            em.detach(entity);
            results.add(entity);
        }
    }

    private void initQueryResult() {
        if (CollectionUtils.isEmpty(results)) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }
    }

}
```
- AbstractPagingItemReader를 상속받았다.
- AbstractPagingItemReader는 어댑터 패턴으로, 상속 받는 쪽은 doReadPage만 구현하면 됨
- name: ItemReader를 구분하기 위한 이름
- entityManagerFactory: JPA를 이용하기 위해서 entityManagerFactory 전달
- Function<JPAQueryFactory, JPAQuery>: JPAQeury를 생성하기 위한 Functional Interface
  - 입력 파라미터로 JPAQueryFactory를 입력으로 전달 받음
  - 반환값은 JPAQuery 형태의 querydsl 쿼리가 됨
- chunkSize: 한번에 페이징 처리할 페이지 크기
- alwaysReadFromZero: 항상 0부터 페이징을 읽을지 여부를 지정. 만약 paging 처리된 데이터 자체를 수정하는 경우 배치처리 누락이 발생할 수 있으므로 이를 해결하기 위한 방안
- doClose()
  - 기본적으로 AbstractPagingItemReader에 자체 구현되어 있지만 EntityManager 자원을 해제하기 위해 em.close()를 수행함
- doReadPage()
  - 실제로 우리가 구현해야할 추상 메소드
```java
@Override
protected void doReadPage() {
    initQueryResult();

    JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);
    long offset = 0;
    if (!alwaysReadFromZero) {
        offset = (long) getPage() * getPageSize();
    }

    JPAQuery<T> query = querySupplier.apply(jpaQueryFactory).offset(offset).limit(getPageSize());

    List<T> queryResult = query.fetch();
    for (T entity: queryResult) {
        em.detach(entity);
        results.add(entity);
    }
}

private void initQueryResult() {
    if (CollectionUtils.isEmpty(results)) {
        results = new CopyOnWriteArrayList<>();
    } else {
        results.clear();
    }
}
```
- JPAQueryFactory를 통해서 함수형 인터페이스로 지정된 querydsl에 적용할 QueryFactory이다.
- 만약 alwaysReadFromZero가 false라면 offset과 limit을 계속 이동하면서 조회하도록 offset을 계산한다.
- querySupplier.apply
  - 우리가 제공한 querySupplier에 JPAQueryFactory를 적용하여 JPAQuery를 생성하도록 한다.
  - 페이징을 위해서 offset, limit을 계산된 offset과 pageSize (청크크기)를 지정하여 페이징 처리하도록 한다.
- fetch
  - 결과를 패치하여 패치된 내역을 result에 담는다.
  - 이때 entityManager에서 detech하여 변경이 실제 DB에 반영되지 않도록 영속성 객체에서 제외시킨다.
- initQueryResult
  - 매 페이징 결과를 반환할 때 페이징 결과만 반환하기 위해서 초기화
  - 만약 결과 객체가 초기화 되어 있지 않다면 CopyOnWriteArrayList 객체를 신규로 생성한다.

## 편의를 위해서 Builder 생성하기

---
```java
public class QuerydslPagingItemReaderBuilder<T> {
    private EntityManagerFactory emf;
    private Function<JPAQueryFactory, JPAQuery<T>> querySupplier;

    private int chunkSize = 10;
    private String name;
    private Boolean alwaysReadFromZero;

    public QuerydslPagingItemReaderBuilder<T> entityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
        return this;
    }

    public QuerydslPagingItemReaderBuilder<T> querySupplier(Function<JPAQueryFactory, JPAQuery<T>> querySupplier) {
        this.querySupplier = querySupplier;
        return this;
    }

    public QuerydslPagingItemReaderBuilder<T> chunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    public QuerydslPagingItemReaderBuilder<T> name(String name) {
        this.name = name;
        return this;
    }

    public QuerydslPagingItemReaderBuilder<T> alwaysReadFromZero(boolean alwaysReadFromZero) {
        this.alwaysReadFromZero = alwaysReadFromZero;
        return this;
    }

    public QuerydslPagingItemReader<T> build() {
        if (name == null) {
            this.name = ClassUtils.getShortName(QuerydslPagingItemReader.class);
        }
        if (this.emf == null) {
            throw new IllegalArgumentException("EntityManagerFactory can not be null.!");
        }
        if (this.querySupplier == null) {
            throw new IllegalArgumentException("Function<JPAQueryFactory, JPAQuery<T>> can not be null.!");
        }
        if (this.alwaysReadFromZero == null) {
            alwaysReadFromZero = false;
        }
        return new QuerydslPagingItemReader<>(this.name, emf, querySupplier, chunkSize, alwaysReadFromZero);
    }
}
```

## CustomItemWriter 개요

---
- SpringBatch에서 제공하는 기본 ItemWriter 인터페이스를 구현하여 직접 작성해보자
- 기본 ItemWriter 클래스로는 제공되지 않는 특정 기능을 구현할 때 사용됨

### 구성요소
- ItemWriter 인터페이스 구현: write()메소드를 구현하여 원하는 처리를 수행
- 필요한 라이브러리 및 객체 선언: 사용할 라이브러리 및 객체를 선언
- 데이터 처리 로직 구현: write() 메소드에서 데이터 처리 로직을 구현

### 장점
- 유연성: 기본 ItemWriter 클래스로는 제공되지 않는 특정 기능을 구현할 수 있음
- 확장성: 다양한 방식으로 데이터 처리를 확장할 수 있음
- 제어 가능성: 데이터 처리 과정을 완벽하게 제어할 수 있음

### 단점
- 개발 복잡성: 기본 ItemWriter 클래스보다 개발 과정이 더 복잡
- 테스트 어려움: 테스트 작성이 더 어려울 수 있음
- 디버깅 어려움: 문제 발생시 디거빙이 더 어려울 수 있음

## 소스 샘픔

---
### 임시 CustomerAPI 생성
- Grade기 A인 사람은 보너스를 20000, 아닌 경우 10000을 준다.
```java
@Component
public class CustomerRemoteApi {
    public Map<String, Integer> getBonus(Customer customer) {
        Integer bonus;
        if (Customer.Grade.A == customer.getGrade()) {
            bonus = 20000;
        } else {
            bonus = 10000;
        }

        return Map.of("code", 200, "bonus", bonus);
    }
}
```

### CustomerItemWriter 작성하기
- ItemWriter 이넡페이스를 구현
```java
@Slf4j
@Component
public class CustomWeek9ItemWriter implements ItemWriter<Customer> {
    private final CustomerRemoteApi customerRemoteApi;

    public CustomWeek9ItemWriter(CustomerRemoteApi customerRemoteApi) {
        this.customerRemoteApi = customerRemoteApi;
    }

    @Override
    public void write(Chunk<? extends Customer> chunk) {
        for (Customer customer: chunk) {
            Map<String, Integer> response = customerRemoteApi.getBonus(customer);
            Integer code = response.getOrDefault("code", 503);
            Integer bonus = response.getOrDefault("bonus", 0);

            if (code == 200) {
                if (bonus > 15000) {
                    System.out.println("보너스 많이 받은 사람: " + customer.getName());
                }
            } else {
                log.error("api server connection is not available");
            }
        }
    }
}
```
- 이전에 만든 CustomAPI를 생성자 파라미터로 받고, write 메소드를 구현
- write 메소드는 ItemWriter의 핵심 메소드
- Chunk는 Customer 객체를 한묶음으로 처리할 수 있도록 반복 수행할 수 있음. 하여 for 구문을 사용
- 이 메소드 내부에서 processToOtherService를 호출

### 전체 코드

---
```java
@Log
@Configuration
public class CustomWeek9JobConfig {
    public static final int CHUNK_SIZE = 10;
    public static final String ENCODING = "UTF-8";
    public static final String QUERYDSL_PAGING_CHUNK_JOB = "QUERYDSL_PAGING_CHUNK_JOB";

    @Autowired
    DataSource dataSource;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    CustomWeek9ItemWriter customWeek9ItemWriter;

    @Bean
    public QuerydslPagingItemReader<Customer> customerQuerydslPagingItemReader() {

        return new QuerydslPagingItemReaderBuilder<Customer>()
                .name("customerQuerydslPagingItemReader")
                .entityManagerFactory(emf)
                .chunkSize(CHUNK_SIZE)
                .querySupplier(jpaQueryFactory -> jpaQueryFactory.select(QCustomer.customer).from(QCustomer.customer).where(QCustomer.customer.age.gt(50)))
                .build();
    }

    @Bean
    public Step customerQuerydslPagingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("-------------------------- Init customerQuerydslPagingStep --------------------------");
        return new StepBuilder("customerJpaPagingStep", jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(customerQuerydslPagingItemReader())
                .writer(customWeek9ItemWriter)
                .build();
    }

    @Bean
    public Job customerQuerydslPagingJob(Step customerQuerydslPagingStep, JobRepository jobRepository) {
        log.info("-------------------------- Init QUERYDSL_PAGING_CHUNK_JOB --------------------------");
        return new JobBuilder(QUERYDSL_PAGING_CHUNK_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerQuerydslPagingStep)
                .build();
    }

}
```

### 실행 결과
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.4)

2024-12-10T13:45:10.254+09:00  INFO 20228 --- [batch] [           main] study.batch.BatchApplication             : Starting BatchApplication using Java 17.0.8.1 with PID 20228 (/Users/1004781/study/spring/batch/build/classes/java/main started by 1004781 in /Users/1004781/study/spring/batch)
2024-12-10T13:45:10.255+09:00  INFO 20228 --- [batch] [           main] study.batch.BatchApplication             : The following 1 profile is active: "local"
2024-12-10T13:45:10.495+09:00  INFO 20228 --- [batch] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2024-12-10T13:45:10.505+09:00  INFO 20228 --- [batch] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 5 ms. Found 0 JPA repository interfaces.
2024-12-10T13:45:10.606+09:00  WARN 20228 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.boot.autoconfigure.jdbc.DataSourceConfiguration$Hikari' of type [org.springframework.boot.autoconfigure.jdbc.DataSourceConfiguration$Hikari] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-12-10T13:45:10.613+09:00  WARN 20228 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'spring.datasource-org.springframework.boot.autoconfigure.jdbc.DataSourceProperties' of type [org.springframework.boot.autoconfigure.jdbc.DataSourceProperties] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-12-10T13:45:10.614+09:00  WARN 20228 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration$PooledDataSourceConfiguration' of type [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration$PooledDataSourceConfiguration] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-12-10T13:45:10.615+09:00  WARN 20228 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'jdbcConnectionDetails' of type [org.springframework.boot.autoconfigure.jdbc.PropertiesJdbcConnectionDetails] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-12-10T13:45:10.625+09:00  WARN 20228 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'dataSource' of type [com.zaxxer.hikari.HikariDataSource] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-12-10T13:45:10.628+09:00  WARN 20228 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'basicTaskJobConfiguration' of type [study.batch.week2.BasicTaskJobConfiguration$$SpringCGLIB$$0] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-12-10T13:45:10.631+09:00  WARN 20228 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'transactionManager' of type [org.springframework.jdbc.datasource.DataSourceTransactionManager] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-12-10T13:45:10.632+09:00  WARN 20228 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'spring.batch-org.springframework.boot.autoconfigure.batch.BatchProperties' of type [org.springframework.boot.autoconfigure.batch.BatchProperties] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-12-10T13:45:10.634+09:00  WARN 20228 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration$SpringBootBatchConfiguration' of type [org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration$SpringBootBatchConfiguration] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). The currently created BeanPostProcessor [jobRegistryBeanPostProcessor] is declared through a non-static factory method on that class; consider declaring it as static instead.
2024-12-10T13:45:10.642+09:00  INFO 20228 --- [batch] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2024-12-10T13:45:11.199+09:00  INFO 20228 --- [batch] [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@6deee370
2024-12-10T13:45:11.200+09:00  INFO 20228 --- [batch] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2024-12-10T13:45:11.241+09:00  INFO 20228 --- [batch] [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2024-12-10T13:45:11.268+09:00  INFO 20228 --- [batch] [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.5.3.Final
2024-12-10T13:45:11.281+09:00  INFO 20228 --- [batch] [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2024-12-10T13:45:11.420+09:00  INFO 20228 --- [batch] [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2024-12-10T13:45:11.809+09:00  INFO 20228 --- [batch] [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2024-12-10T13:45:11.811+09:00  INFO 20228 --- [batch] [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2024-12-10T13:45:11.884+09:00  INFO 20228 --- [batch] [           main] study.batch.week2.GreetingTask           : ----------------------- After Properties Set() -----------------------
2024-12-10T13:45:11.896+09:00  INFO 20228 --- [batch] [           main] s.batch.week2.BasicTaskJobConfiguration  : ------------------ Init myStep -----------------
2024-12-10T13:45:11.907+09:00  INFO 20228 --- [batch] [           main] s.batch.week2.BasicTaskJobConfiguration  : ------------------ Init myJob -----------------
2024-12-10T13:45:11.916+09:00  INFO 20228 --- [batch] [           main] s.batch.week3.PlayerJobConfiguration     : ------------------ Init PlayerStep -----------------
2024-12-10T13:45:11.923+09:00  INFO 20228 --- [batch] [           main] s.batch.week3.PlayerJobConfiguration     : ------------------ Init PlayerJob -----------------
2024-12-10T13:45:11.925+09:00  INFO 20228 --- [batch] [           main] s.b.week4.CustomerFlatJobConfiguration   : ------------------ Init flatFileStep -----------------
2024-12-10T13:45:11.928+09:00  INFO 20228 --- [batch] [           main] s.b.week4.CustomerFlatJobConfiguration   : ------------------ Init flatFileJob -----------------
2024-12-10T13:45:11.934+09:00  INFO 20228 --- [batch] [           main] s.b.week5.CustomerJdbcJobConfiguration   : ------------------ Init customerJdbcPagingStep -----------------
2024-12-10T13:45:11.936+09:00  INFO 20228 --- [batch] [           main] s.b.week5.CustomerJdbcJobConfiguration   : ------------------ Init customerJdbcPagingJob -----------------
2024-12-10T13:45:11.937+09:00  INFO 20228 --- [batch] [           main] s.b.week7.CustomerMybatisConfiguration   : ------------------ Init customerJdbcCursorStep -----------------
2024-12-10T13:45:11.945+09:00  INFO 20228 --- [batch] [           main] s.b.week7.CustomerMybatisConfiguration   : ------------------ Init customerJdbcCursorPagingJob -----------------
2024-12-10T13:45:11.946+09:00  INFO 20228 --- [batch] [           main] s.b.week8.CustomCompositeConfiguration   : ------------------ Init customerJdbcCursorStep -----------------
2024-12-10T13:45:11.948+09:00  INFO 20228 --- [batch] [           main] s.b.week8.CustomCompositeConfiguration   : ------------------ Init compositeJob -----------------
2024-12-10T13:45:11.971+09:00  INFO 20228 --- [batch] [           main] study.batch.week9.CustomWeek9JobConfig   : -------------------------- Init customerQuerydslPagingStep --------------------------
2024-12-10T13:45:11.972+09:00  INFO 20228 --- [batch] [           main] study.batch.week9.CustomWeek9JobConfig   : -------------------------- Init QUERYDSL_PAGING_CHUNK_JOB --------------------------
2024-12-10T13:45:12.041+09:00  INFO 20228 --- [batch] [           main] study.batch.BatchApplication             : Started BatchApplication in 1.939 seconds (process running for 2.217)
2024-12-10T13:45:12.043+09:00  INFO 20228 --- [batch] [           main] o.s.b.a.b.JobLauncherApplicationRunner   : Running default command line with: []
2024-12-10T13:45:12.536+09:00  INFO 20228 --- [batch] [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=QUERYDSL_PAGING_CHUNK_JOB]] launched with the following parameters: [{'run.id':'{value=9, type=class java.lang.Long, identifying=true}'}]
2024-12-10T13:45:12.822+09:00  INFO 20228 --- [batch] [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [customerJpaPagingStep]
Hibernate: 
    select
        c1_0.ID,
        c1_0.AGE,
        c1_0.GRADE,
        c1_0.NAME 
    from
        CUSTOMER c1_0 
    where
        c1_0.AGE>? 
    limit
        ?, ?
보너스 많이 받은 사람: Bob
보너스 많이 받은 사람: Frank
보너스 많이 받은 사람: Jack
보너스 많이 받은 사람: Leo
보너스 많이 받은 사람: Nate
보너스 많이 받은 사람: Olivia
보너스 많이 받은 사람: Paul
보너스 많이 받은 사람: Quinn
보너스 많이 받은 사람: Rachel
보너스 많이 받은 사람: Sam
Hibernate: 
    select
        c1_0.ID,
        c1_0.AGE,
        c1_0.GRADE,
        c1_0.NAME 
    from
        CUSTOMER c1_0 
    where
        c1_0.AGE>? 
    limit
        ?, ?
2024-12-10T13:45:13.559+09:00  INFO 20228 --- [batch] [           main] o.s.batch.core.step.AbstractStep         : Step: [customerJpaPagingStep] executed in 736ms
2024-12-10T13:45:13.763+09:00  INFO 20228 --- [batch] [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=QUERYDSL_PAGING_CHUNK_JOB]] completed with the following parameters: [{'run.id':'{value=9, type=class java.lang.Long, identifying=true}'}] and the following status: [COMPLETED] in 1s152ms
2024-12-10T13:45:13.767+09:00  INFO 20228 --- [batch] [ionShutdownHook] j.LocalContainerEntityManagerFactoryBean : Closing JPA EntityManagerFactory for persistence unit 'default'
2024-12-10T13:45:13.768+09:00  INFO 20228 --- [batch] [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown initiated...
2024-12-10T13:45:13.859+09:00  INFO 20228 --- [batch] [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.

종료 코드 0(으)로 완료된 프로세스
```