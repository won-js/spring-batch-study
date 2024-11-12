# 6주차 SPRING BATCH STUDY
- JpaPagingItemReader로 DB내용 읽고, JpaItemWriter로 DB에 쓰기

## JpaPagingItemReader

---
- JpaPagingItemReader는 Spring Batch에서 제공하는 ItemReader로, JPA를 사용하여 데이터베이스로부터 데이터를 페이지 단위로 읽음
- JPA 기능 활용: JPA 엔티티 기반 데이터 처리, 객체 매핑 자동화 등 JPA의 다양한 기능을 활용할 수 있음
- 쿼리 최적화: JPA 쿼리 기능을 사용하여 최적화된 데이터 읽기가 가능
- 커서 제어: JPA Criteria API를 사용하여 데이터 순회를 제어할 수 있음

## JpaPagingItemReader 주요 구성 요소

---
- EntityManagerFactory: JAP 엔티티 매니저 팩토리를 설정
- JpaQueryProvider: 데이터를 읽을 JPA 쿼리르 제공
- PageSize: 페이지 크기를 설정
- SkippableItemReader: 오류 발생 시 해당 Item을 건너뛸 수 있도록 함
- ReadListener: 읽기 시작, 종료, 오류 발생 등의 이벤트를 처리할 수 있도록 함
- SaveStateCallback: 잠시 중단 시 현재 상태를 저장하여 재시작 시 이어서 처리할 수 있도록 함

## JpaItemWriter 구성 요소

---
- EntityManagerFactory: JPA EntityManager 생성을 위한 팩토리 객체
- JpaQueryProvider: 저장할 엔티티를 위한 JPA쿼릴르 생성하는 역할

### 장점
- ORM 연동: JPA를 통해 다양한 데이터베이스에 데이터를 저장할 수 있다.
- 객체 매핑: 엔티티 객체를 직접 저장하여 코드 간결성을 높일 수 있다.
- 유연성: 다양한 설정을 통해 원하는 방식으로 데이터를 저장할 수 있다.

### 단점
- 설정 복잡성: JPA 설정 및 쿼리 작성이 복잡할 수 있다.
- 데이터베이스 종속: 특정 데이터베이스에 종속적이다.
- 오류 가능성: 설정 오류 시 데이터 손상 가능성이 있다.

## 테스트 해보기

--- 

### EntityClass 생성
저번에 사용하였던 Customer.java JPA 맞는 설정과 addOneAge 메소드 추가
```java
@Getter
@Setter
@ToString
@Entity(name = "CUSTOMER")
@Table(name="CUSTOMER")
public class Customer {
    @Id
    @Column(name="ID")
    private  int id;
    @Column(name="NAME")
    private String name;
    @Column(name = "AGE")
    private int age;
    @Enumerated(EnumType.STRING)
    @Column(name="GRADE")
    private Grade grade;

    public enum Grade {
        A, B, C, D
    }

    public String getGrade() {
        return grade.toString();
    }

    public void addOneAge() {
        age++;
    }

    public void assignGroup() {
        if (50 <= age) {
            grade = Grade.A;
        } else if (40 <= age) {
            grade = Grade.B;
        } else if (30 <= age) {
            grade = Grade.C;
        } else {
            grade = Grade.D;
        }
    }
}
```

### JpaItemReader 사용해보기
전체 코드
```java
@Log
@Configuration
public class CustomerJpaJobConfiguration {
    private static final int CHUNK_SIZE = 10;
    private static final String JPA_PAGING_JOB = "JpaPagingJob";
    private static final String JPA_PAGING_STEP = "JpaPagingStep";
    private static final String JPA_PAGING_ITEM_READER = "JpaPagingItemReader";

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Bean
    public JpaPagingItemReader<Customer> customerJpaPagingItemReader() {
        return new JpaPagingItemReaderBuilder<Customer>()
                .name(JPA_PAGING_ITEM_READER)
                .queryString("SELECT c FROM CUSTOMER c WHERE c.age > :age ORDER BY ID DESC")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .parameterValues(Collections.singletonMap("age",20))
                .build();
    }

    @Bean
    public JpaItemWriter<Customer> jpaItemWriter() {
        return new JpaItemWriterBuilder<Customer>()
                .entityManagerFactory(entityManagerFactory)
                .usePersist(true)
                .build();
    }

    @Bean
    public Step customerJpaPagingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("------------------ Init customerJpaPagingStep -----------------");
        return new StepBuilder(JPA_PAGING_STEP, jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(customerJpaPagingItemReader())
                .processor(item -> {
                    log.info("======================= process =======================");
                    item.addOneAge();
                    return item;
                })
                .writer(System.out::println)
                .build();
    }

    @Bean
    public Job customerJpaPagingJob(Step customerJpaPagingStep, JobRepository jobRepository) {
        log.info("------------------ Init customerJpaPagingJob -----------------");
        return new JobBuilder(JPA_PAGING_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerJpaPagingStep)
                .build();
    }
}
```
- queryString: JPQL 쿼리 이용
- entityManager: JPA를 위한 엔티티 매니저 지정
- pageSize: 한번에 읽어올 페이지 크기 - 일반적으로 청크 크기와 동일하게 설정
- parameterValues: JPQL쿼리에 전달할 파라미터 지정

*실행 결과 확인*
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.4)

2024-11-12T16:52:02.605+09:00  INFO 23849 --- [batch] [           main] study.batch.BatchApplication             : Starting BatchApplication using Java 17.0.8.1 with PID 23849 (/Users/1004781/study/spring/batch/build/classes/java/main started by 1004781 in /Users/1004781/study/spring/batch)
2024-11-12T16:52:02.606+09:00  INFO 23849 --- [batch] [           main] study.batch.BatchApplication             : The following 1 profile is active: "local"
2024-11-12T16:52:02.804+09:00  INFO 23849 --- [batch] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2024-11-12T16:52:02.815+09:00  INFO 23849 --- [batch] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 7 ms. Found 0 JPA repository interfaces.
2024-11-12T16:52:02.900+09:00  WARN 23849 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.boot.autoconfigure.jdbc.DataSourceConfiguration$Hikari' of type [org.springframework.boot.autoconfigure.jdbc.DataSourceConfiguration$Hikari] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-12T16:52:02.907+09:00  WARN 23849 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'spring.datasource-org.springframework.boot.autoconfigure.jdbc.DataSourceProperties' of type [org.springframework.boot.autoconfigure.jdbc.DataSourceProperties] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-12T16:52:02.907+09:00  WARN 23849 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration$PooledDataSourceConfiguration' of type [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration$PooledDataSourceConfiguration] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-12T16:52:02.908+09:00  WARN 23849 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'jdbcConnectionDetails' of type [org.springframework.boot.autoconfigure.jdbc.PropertiesJdbcConnectionDetails] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-12T16:52:02.916+09:00  WARN 23849 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'dataSource' of type [com.zaxxer.hikari.HikariDataSource] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-12T16:52:02.919+09:00  WARN 23849 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'basicTaskJobConfiguration' of type [study.batch.week2.BasicTaskJobConfiguration$$SpringCGLIB$$0] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-12T16:52:02.920+09:00  WARN 23849 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'transactionManager' of type [org.springframework.jdbc.datasource.DataSourceTransactionManager] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-12T16:52:02.921+09:00  WARN 23849 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'spring.batch-org.springframework.boot.autoconfigure.batch.BatchProperties' of type [org.springframework.boot.autoconfigure.batch.BatchProperties] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-12T16:52:02.924+09:00  WARN 23849 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration$SpringBootBatchConfiguration' of type [org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration$SpringBootBatchConfiguration] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). The currently created BeanPostProcessor [jobRegistryBeanPostProcessor] is declared through a non-static factory method on that class; consider declaring it as static instead.
2024-11-12T16:52:02.932+09:00  INFO 23849 --- [batch] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2024-11-12T16:52:03.159+09:00  INFO 23849 --- [batch] [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@5c723f2d
2024-11-12T16:52:03.160+09:00  INFO 23849 --- [batch] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2024-11-12T16:52:03.199+09:00  INFO 23849 --- [batch] [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2024-11-12T16:52:03.219+09:00  INFO 23849 --- [batch] [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.5.3.Final
2024-11-12T16:52:03.231+09:00  INFO 23849 --- [batch] [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2024-11-12T16:52:03.366+09:00  INFO 23849 --- [batch] [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2024-11-12T16:52:03.757+09:00  INFO 23849 --- [batch] [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2024-11-12T16:52:03.758+09:00  INFO 23849 --- [batch] [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2024-11-12T16:52:03.763+09:00  INFO 23849 --- [batch] [           main] study.batch.week2.GreetingTask           : ----------------------- After Properties Set() -----------------------
2024-11-12T16:52:03.774+09:00  INFO 23849 --- [batch] [           main] s.batch.week2.BasicTaskJobConfiguration  : ------------------ Init myStep -----------------
2024-11-12T16:52:03.785+09:00  INFO 23849 --- [batch] [           main] s.batch.week2.BasicTaskJobConfiguration  : ------------------ Init myJob -----------------
2024-11-12T16:52:03.798+09:00  INFO 23849 --- [batch] [           main] s.batch.week3.PlayerJobConfiguration     : ------------------ Init PlayerStep -----------------
2024-11-12T16:52:03.805+09:00  INFO 23849 --- [batch] [           main] s.batch.week3.PlayerJobConfiguration     : ------------------ Init PlayerJob -----------------
2024-11-12T16:52:03.807+09:00  INFO 23849 --- [batch] [           main] s.b.week4.CustomerFlatJobConfiguration   : ------------------ Init flatFileStep -----------------
2024-11-12T16:52:03.814+09:00  INFO 23849 --- [batch] [           main] s.b.week4.CustomerFlatJobConfiguration   : ------------------ Init flatFileJob -----------------
2024-11-12T16:52:03.820+09:00  INFO 23849 --- [batch] [           main] s.b.week5.CustomerJdbcJobConfiguration   : ------------------ Init customerJdbcPagingStep -----------------
2024-11-12T16:52:03.822+09:00  INFO 23849 --- [batch] [           main] s.b.week5.CustomerJdbcJobConfiguration   : ------------------ Init customerJdbcPagingJob -----------------
2024-11-12T16:52:03.823+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ------------------ Init customerJpaPagingStep -----------------
2024-11-12T16:52:03.825+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ------------------ Init customerJpaPagingJob -----------------
2024-11-12T16:52:03.891+09:00  INFO 23849 --- [batch] [           main] study.batch.BatchApplication             : Started BatchApplication in 1.426 seconds (process running for 1.679)
2024-11-12T16:52:03.892+09:00  INFO 23849 --- [batch] [           main] o.s.b.a.b.JobLauncherApplicationRunner   : Running default command line with: []
2024-11-12T16:52:04.328+09:00  INFO 23849 --- [batch] [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=JpaPagingJob]] launched with the following parameters: [{'run.id':'{value=40, type=class java.lang.Long, identifying=true}'}]
2024-11-12T16:52:04.573+09:00  INFO 23849 --- [batch] [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [JpaPagingStep]
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
    order by
        c1_0.ID desc 
    limit
        ?, ?
2024-11-12T16:52:04.889+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:04.889+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:04.889+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:04.889+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:04.889+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:04.889+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:04.889+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:04.889+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:04.889+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:04.889+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
[items=[Customer(id=20, name=Tina, age=32, grade=D), Customer(id=19, name=Sam, age=38, grade=C), Customer(id=18, name=Rachel, age=34, grade=D), Customer(id=17, name=Quinn, age=31, grade=D), Customer(id=16, name=Paul, age=40, grade=C), Customer(id=15, name=Olivia, age=33, grade=D), Customer(id=14, name=Nate, age=35, grade=D), Customer(id=13, name=Mia, age=27, grade=D), Customer(id=12, name=Leo, age=39, grade=C), Customer(id=11, name=Kate, age=29, grade=D)], skips=[]]
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
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
    order by
        c1_0.ID desc 
    limit
        ?, ?
2024-11-12T16:52:05.080+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:05.080+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:05.080+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:05.080+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:05.080+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:05.080+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:05.080+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:05.080+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:05.081+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
2024-11-12T16:52:05.081+09:00  INFO 23849 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ======================= process =======================
[items=[Customer(id=10, name=Jack, age=37, grade=C), Customer(id=9, name=Ivy, age=32, grade=D), Customer(id=8, name=Hank, age=34, grade=D), Customer(id=7, name=Grace, age=30, grade=D), Customer(id=6, name=Frank, age=41, grade=C), Customer(id=5, name=Eve, age=35, grade=D), Customer(id=4, name=Diana, age=33, grade=D), Customer(id=3, name=Charlie, age=28, grade=D), Customer(id=2, name=Bob, age=36, grade=C), Customer(id=1, name=Alice, age=31, grade=D)], skips=[]]
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
Hibernate: 
    update
        CUSTOMER 
    set
        AGE=?,
        GRADE=?,
        NAME=? 
    where
        ID=?
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
    order by
        c1_0.ID desc 
    limit
        ?, ?
2024-11-12T16:52:05.362+09:00  INFO 23849 --- [batch] [           main] o.s.batch.core.step.AbstractStep         : Step: [JpaPagingStep] executed in 788ms
2024-11-12T16:52:05.522+09:00  INFO 23849 --- [batch] [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=JpaPagingJob]] completed with the following parameters: [{'run.id':'{value=40, type=class java.lang.Long, identifying=true}'}] and the following status: [COMPLETED] in 1s117ms
2024-11-12T16:52:05.530+09:00  INFO 23849 --- [batch] [ionShutdownHook] j.LocalContainerEntityManagerFactoryBean : Closing JPA EntityManagerFactory for persistence unit 'default'
2024-11-12T16:52:05.533+09:00  INFO 23849 --- [batch] [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown initiated...
2024-11-12T16:52:05.618+09:00  INFO 23849 --- [batch] [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.
```
사실 writer에 print기능을 넣어 놔서 따로 DB 데이터가 변경되지 않을 것 이라고 생각을 했는데 hibernate에 의하여 변경사항이 모두 없데이트 되었다.

**❓그렇다면 Hibernate를 사용하는 경우 ItemWriter는 어떤 역할을 해야할까 (찾아보기)❓** <br>

### Ref
- https://devocean.sk.com/blog/techBoardDetail.do?ID=166902
