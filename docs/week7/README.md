# 7주차 SPRING BATCH STUDY
- MyBatisPagingItemReader로 DB내용을 읽고, MyBatisItemWriter로 DB에 쓰기

## MyBatisItemReader

---
MyBatisPagingItemReader Spring Mybatis에서 제공하는 ItemReader 인터페이스를 구현하는 클래스

- 장점
  - 간편한 설정 - MyBatis 쿼리 매퍼를 직접 활용하여 데이터를 읽을 수 있어 설정이 간편함
  - 쿼리 최적화 - MyBatis의 다양한 기능을 활용하여 최적화된 쿼리를 작성 가능
  - 동적 쿼리 지원 - 런타임 시 조건에 따라 동적으로 쿼리 생성 가능
- 단점
  - MyBatis 의존성 - MyBatis 라이브러리에 의존해야함
  - 커스터마이징 복잡 - Chunk-oriented Processing 방식과 비교했을때 커스터마이징이 더 복잡할 수 있음

### 주요 구성 요소

---
- SqlSessionFactory - MyBatis 설정 정보 및 SQL 쿼리 매퍼 정보를 담고 있는 객체
- QueryId - 데이터를 읽을 MyBatis 쿼리 ID
- PageSize - 페이징 쿼리를 위한 페이지 크기를 지정

1. SqlSessionFactory
- MyBatisPagingItemReader SqlSessionFactory 객체를 통해 MyBatis와 연동
- SqlSessionFactory는 다음과 같은 방법으로 설정 가능
  - @Bean 어노테이션을 사용하여 직접 생성
  - Spring Batch XML 설정 파일에서 생성
  - Java 코드에서 직접 설정
2. QueryId
- MyBatisPagingItemReader setQueryId() 메소드를 통해 데이터를 읽을 MyBatis 쿼리 ID를 설정
3. PageSize
- MyBatisItemReader는 pageSize를 이용하여 offset, limit을 이용하는 기준을 설정할 수 있음
4. 추가 구성 요소
- SkippableItemReader - 오류 발생시 Item을 건너뛸 수 있도록 함
- ReadListener - 읽기 시작, 종료, 오류 발생 등의 이벤트를 처리할 수 있도록 함
- SaveStateCallback - 잡의 중단 시 현재 상태를 저장하여 재시작 시 이어서 처리할 수 있도록 함

## MyBatisItemWriter
MyBatisBatchItemWriter Spring Batch에서 제공하는 ItemWriter 인터페이스를 구현하는 클래스<br>
데이터를 MyBatis를 통해 데이터베이스에 저장하는 데 사용

### 주요 구성 요소

---
- SqlSessionTemplate - MyBatis SqlSession 생성 및 관리를 위한 템플릿 객체
- SqlSessionFactory - SqlSessionTemplate 생성을 위한 팩토리 객체
- StatementId - 실행할 MyBatis SQL 맵퍼의 statement ID
- ItemToParameterConverter - 객체를 ParameterMap으로 변경 가능

- 장점
  - ORM 연동 - MyBatis를 통해 다양한 데이터베이스에 데이터를 저장할 수 있음
  - SQL 쿼리 분리 - SQL 쿼리를 Java 코드로부터 분리하여 관리 및 유지 보수가 용이
  - 유연성 - 다양한 설정을 통해 원하는 방식으로 데이터를 저장할 수 있음
- 단점
  - 설정 복잡성 - MyBatis 설정 및 SQL Mapper 작성이 복잡할 수 있음
  - 데이터베이스 종속 - 특정 데이터베이스에 종속적 (쿼리 작성)
  - 오류 가능성 - 설정 오류 시 데이터 손상 가능성이 있음

## 활용하기

---
기존 Customer를 읽어오고 Age+1하여 저장한다.

### Mybatis xml 작성
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="study.batch.week7.MybatisMapper">
	<resultMap id="customerResult" type="study.batch.common.Customer">
		<result property="id" column="id"/>
		<result property="name" column="name"/>
		<result property="age" column="age"/>
		<result property="grade" column="grade"/>
	</resultMap>

	<select id="selectCustomers" resultMap="customerResult">
		SELECT id, name, age, grade
		FROM CUSTOMER
		LIMIT #{_skiprows}, #{_pagesize}
	</select>
	<update id="updateCustomer">
		UPDATE CUSTOMER SET age=#{age} WHERE id=#{id}
	</update>
</mapper>
```
- selectCustomers - customer 데이터를 읽어옴
- updateCustomer - customer 데이터를 업데이트

### xml 파일 위치 설정
```properties
mybatis.mapper-locations=classpath:week7/*.xml
```
application에 mybatis xml이 있는 위치를 알려준다.

### MybatisMapper 설정
```java
@Mapper
public interface MybatisMapper {
    List<Customer> selectCustomers();
    int updateCustomer();
}
```

### 전체 Job 코드 작성 (Reader / Processor / Writer)
```java
@Log
@Configuration
public class CustomerMybatisConfiguration {
    private static final int CHUNK_SIZE = 10;
    private static final String MYBATIS_CHUNK_JOB = "MybatisChunkJob";

    @Autowired
    DataSource dataSource;

    @Autowired
    SqlSessionFactory sqlSessionFactory;

    @Bean
    public MyBatisPagingItemReader<Customer> myBatisItemReader() {
        return new MyBatisPagingItemReaderBuilder<Customer>()
                .sqlSessionFactory(sqlSessionFactory)
                .pageSize(CHUNK_SIZE)
                .queryId("study.batch.week7.MybatisMapper.selectCustomers")
                .build();
    }

    public MyBatisBatchItemWriter<Customer> myBatisItemWriter() {
        return new MyBatisBatchItemWriterBuilder<Customer>()
                .sqlSessionFactory(sqlSessionFactory)
                .statementId("study.batch.week7.MybatisMapper.updateCustomer")
                .itemToParameterConverter(item -> {
                    Map<String, Object> parameter = new HashMap<>();
                    parameter.put("id", item.getId());
                    parameter.put("age", item.getAge());
                    return parameter;
                })
                .build();
    }

    @Bean
    public Step customerJdbcCursorStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        log.info("------------------ Init customerJdbcCursorStep -----------------");

        return new StepBuilder("customerJdbcCursorStep", jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(myBatisItemReader())
                .processor(item -> {
                    System.out.println("==================" + item.getName() + "==================");
                    System.out.println("Before Age: " + item.getAge());
                    item.addOneAge();
                    System.out.println("After Age: " + item.getAge());
                    return item;
                })
                .writer(myBatisItemWriter())
                .build();
    }

    @Bean
    public Job customerJdbcCursorPagingJob(Step customerJdbcCursorStep, JobRepository jobRepository) {
        log.info("------------------ Init customerJdbcCursorPagingJob -----------------");
        return new JobBuilder(MYBATIS_CHUNK_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerJdbcCursorStep)
                .build();
    }
}
```

### 실제 수행 및 확인
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.4)

2024-11-19T17:36:43.204+09:00  INFO 25847 --- [batch] [           main] study.batch.BatchApplication             : Starting BatchApplication using Java 17.0.8.1 with PID 25847 (/Users/1004781/study/spring/batch/build/classes/java/main started by 1004781 in /Users/1004781/study/spring/batch)
2024-11-19T17:36:43.206+09:00  INFO 25847 --- [batch] [           main] study.batch.BatchApplication             : The following 1 profile is active: "local"
2024-11-19T17:36:43.436+09:00  INFO 25847 --- [batch] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2024-11-19T17:36:43.445+09:00  INFO 25847 --- [batch] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 5 ms. Found 0 JPA repository interfaces.
2024-11-19T17:36:43.542+09:00  WARN 25847 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.boot.autoconfigure.jdbc.DataSourceConfiguration$Hikari' of type [org.springframework.boot.autoconfigure.jdbc.DataSourceConfiguration$Hikari] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-19T17:36:43.550+09:00  WARN 25847 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'spring.datasource-org.springframework.boot.autoconfigure.jdbc.DataSourceProperties' of type [org.springframework.boot.autoconfigure.jdbc.DataSourceProperties] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-19T17:36:43.550+09:00  WARN 25847 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration$PooledDataSourceConfiguration' of type [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration$PooledDataSourceConfiguration] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-19T17:36:43.551+09:00  WARN 25847 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'jdbcConnectionDetails' of type [org.springframework.boot.autoconfigure.jdbc.PropertiesJdbcConnectionDetails] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-19T17:36:43.559+09:00  WARN 25847 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'dataSource' of type [com.zaxxer.hikari.HikariDataSource] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-19T17:36:43.562+09:00  WARN 25847 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'basicTaskJobConfiguration' of type [study.batch.week2.BasicTaskJobConfiguration$$SpringCGLIB$$0] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-19T17:36:43.564+09:00  WARN 25847 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'transactionManager' of type [org.springframework.jdbc.datasource.DataSourceTransactionManager] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-19T17:36:43.565+09:00  WARN 25847 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'spring.batch-org.springframework.boot.autoconfigure.batch.BatchProperties' of type [org.springframework.boot.autoconfigure.batch.BatchProperties] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-11-19T17:36:43.569+09:00  WARN 25847 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration$SpringBootBatchConfiguration' of type [org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration$SpringBootBatchConfiguration] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). The currently created BeanPostProcessor [jobRegistryBeanPostProcessor] is declared through a non-static factory method on that class; consider declaring it as static instead.
2024-11-19T17:36:43.577+09:00  INFO 25847 --- [batch] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2024-11-19T17:36:43.803+09:00  INFO 25847 --- [batch] [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@7ee8130e
2024-11-19T17:36:43.804+09:00  INFO 25847 --- [batch] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2024-11-19T17:36:43.846+09:00  INFO 25847 --- [batch] [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2024-11-19T17:36:43.871+09:00  INFO 25847 --- [batch] [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.5.3.Final
2024-11-19T17:36:43.885+09:00  INFO 25847 --- [batch] [           main] o.h.c.internal.RegionFactoryInitiator    : HHH000026: Second-level cache disabled
2024-11-19T17:36:44.012+09:00  INFO 25847 --- [batch] [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2024-11-19T17:36:44.398+09:00  INFO 25847 --- [batch] [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2024-11-19T17:36:44.399+09:00  INFO 25847 --- [batch] [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2024-11-19T17:36:44.482+09:00  INFO 25847 --- [batch] [           main] study.batch.week2.GreetingTask           : ----------------------- After Properties Set() -----------------------
2024-11-19T17:36:44.494+09:00  INFO 25847 --- [batch] [           main] s.batch.week2.BasicTaskJobConfiguration  : ------------------ Init myStep -----------------
2024-11-19T17:36:44.506+09:00  INFO 25847 --- [batch] [           main] s.batch.week2.BasicTaskJobConfiguration  : ------------------ Init myJob -----------------
2024-11-19T17:36:44.518+09:00  INFO 25847 --- [batch] [           main] s.batch.week3.PlayerJobConfiguration     : ------------------ Init PlayerStep -----------------
2024-11-19T17:36:44.527+09:00  INFO 25847 --- [batch] [           main] s.batch.week3.PlayerJobConfiguration     : ------------------ Init PlayerJob -----------------
2024-11-19T17:36:44.528+09:00  INFO 25847 --- [batch] [           main] s.b.week4.CustomerFlatJobConfiguration   : ------------------ Init flatFileStep -----------------
2024-11-19T17:36:44.531+09:00  INFO 25847 --- [batch] [           main] s.b.week4.CustomerFlatJobConfiguration   : ------------------ Init flatFileJob -----------------
2024-11-19T17:36:44.540+09:00  INFO 25847 --- [batch] [           main] s.b.week5.CustomerJdbcJobConfiguration   : ------------------ Init customerJdbcPagingStep -----------------
2024-11-19T17:36:44.542+09:00  INFO 25847 --- [batch] [           main] s.b.week5.CustomerJdbcJobConfiguration   : ------------------ Init customerJdbcPagingJob -----------------
2024-11-19T17:36:44.544+09:00  INFO 25847 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ------------------ Init customerJpaPagingStep -----------------
2024-11-19T17:36:44.546+09:00  INFO 25847 --- [batch] [           main] s.b.week6.CustomerJpaJobConfiguration    : ------------------ Init customerJpaPagingJob -----------------
2024-11-19T17:36:44.548+09:00  INFO 25847 --- [batch] [           main] s.b.week7.CustomerMybatisConfiguration   : ------------------ Init customerJdbcCursorStep -----------------
2024-11-19T17:36:44.557+09:00  INFO 25847 --- [batch] [           main] s.b.week7.CustomerMybatisConfiguration   : ------------------ Init customerJdbcCursorPagingJob -----------------
2024-11-19T17:36:44.636+09:00  INFO 25847 --- [batch] [           main] study.batch.BatchApplication             : Started BatchApplication in 1.573 seconds (process running for 1.791)
2024-11-19T17:36:44.638+09:00  INFO 25847 --- [batch] [           main] o.s.b.a.b.JobLauncherApplicationRunner   : Running default command line with: []
2024-11-19T17:36:45.046+09:00  INFO 25847 --- [batch] [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=MybatisChunkJob]] launched with the following parameters: [{'run.id':'{value=21, type=class java.lang.Long, identifying=true}'}]
2024-11-19T17:36:45.290+09:00  INFO 25847 --- [batch] [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [customerJdbcCursorStep]
==================Alice==================
Before Age: 45
After Age: 46
==================Bob==================
Before Age: 50
After Age: 51
==================Charlie==================
Before Age: 42
After Age: 43
==================Diana==================
Before Age: 47
After Age: 48
==================Eve==================
Before Age: 49
After Age: 50
==================Frank==================
Before Age: 55
After Age: 56
==================Grace==================
Before Age: 44
After Age: 45
==================Hank==================
Before Age: 48
After Age: 49
==================Ivy==================
Before Age: 46
After Age: 47
==================Jack==================
Before Age: 51
After Age: 52
==================Kate==================
Before Age: 49
After Age: 50
==================Leo==================
Before Age: 59
After Age: 60
==================Mia==================
Before Age: 47
After Age: 48
==================Nate==================
Before Age: 55
After Age: 56
==================Olivia==================
Before Age: 53
After Age: 54
==================Paul==================
Before Age: 60
After Age: 61
==================Quinn==================
Before Age: 51
After Age: 52
==================Rachel==================
Before Age: 54
After Age: 55
==================Sam==================
Before Age: 58
After Age: 59
==================Tina==================
Before Age: 52
After Age: 53
2024-11-19T17:36:45.830+09:00  INFO 25847 --- [batch] [           main] o.s.batch.core.step.AbstractStep         : Step: [customerJdbcCursorStep] executed in 538ms
2024-11-19T17:36:45.987+09:00  INFO 25847 --- [batch] [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=MybatisChunkJob]] completed with the following parameters: [{'run.id':'{value=21, type=class java.lang.Long, identifying=true}'}] and the following status: [COMPLETED] in 863ms
2024-11-19T17:36:45.992+09:00  WARN 25847 --- [batch] [ionShutdownHook] o.s.b.f.support.DisposableBeanAdapter    : Custom destroy method 'close' on bean with name 'customerJpaPagingItemReader' propagated an exception: org.springframework.batch.item.ItemStreamException: Error while closing item reader
2024-11-19T17:36:45.992+09:00  INFO 25847 --- [batch] [ionShutdownHook] j.LocalContainerEntityManagerFactoryBean : Closing JPA EntityManagerFactory for persistence unit 'default'
2024-11-19T17:36:45.995+09:00  INFO 25847 --- [batch] [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown initiated...
2024-11-19T17:36:46.090+09:00  INFO 25847 --- [batch] [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.

종료 코드 0(으)로 완료된 프로세스
```
- 나이가 한살씩 늘어났고 해당 데이터로 DB도 변경된 것을 확인하였음

### 참조
- https://devocean.sk.com/blog/techBoardDetail.do?ID=166932