# 2주차 SPRING BATCH STUDY

## Basic Spring Batch Application

---

### 기본 배치 어플리케이션
- 1주차에서 생성한 스프링 배치를 그대로 사용
  - 처음 실습은 H2를 사용
  - Tasklet 구현체 생성
  - @Configuration을 통해서 생성할 빈을 스프링에 등록
  - Job, Step을 생성하고 빈을 등록
  - 실행 결과 확인

### Tasklet 구현체 생성하기

```java
package study.batch.jobs.task01;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;

public class GreetingTask implements Tasklet, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(GreetingTask.class);

    @Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) throws Exception {
        logger.info("----------------------- Task Execute -----------------------");
        logger.info("GreetingTask : {}, {}", contribution, chunkContext);

        return RepeatStatus.FINISHED;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("----------------------- After Properties Set() -----------------------");
    }
}

```
- Tasklet과 InitializeBean 인터페이스를 구현
- Tasklet은 execute 메소드, InitializeBean은 afterPropertiesSet 메소드를 구현해야 함
  - execute
    - StepContribution과 ChunkContext를 파라미터로 받음
    - 최종적으로 RepeatStatus를 반환
      - FINISHIED: Tasklet이 종료되었음
      - CONTINUABLE: 계속해서 테스크를 수행하도록 함
      - continueIf(condition): 조건에 따라 종료할지 지속할지 결정하는 메소드
  - afterPropertiesSet
    - task를 수행할 때 프로퍼티를 설정하고 난 뒤에 수행되는 메소드
    - 수행할 것이 없다면 비어도 됨

### 필요한 설정, Job, Step을 생성하고 빈에 등록
```java
package study.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;
import study.batch.jobs.task01.GreetingTask;

import javax.sql.DataSource;

@Configuration
public class BasicTaskJobConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(BasicTaskJobConfiguration.class);

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    public Tasklet greetingTasklet() {
        return new GreetingTask();
    }

    @Bean
    public Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        logger.info("------------------ Init myStep -----------------");

        return new StepBuilder("myStep", jobRepository)
                .tasklet(greetingTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Job myJob(Step step, JobRepository jobRepository) {
        logger.info("------------------ Init myJob -----------------");
        return new JobBuilder("myJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step)
                .build();
    }
}
```
- DataSource
  - 지금 실습에서는 H2를 사용할 것이므로 H2에 대한 datasource 설정
- PlatformTransactionManager
  - 트랜잭션 관리를 위한 구현체
  - 앞서 정의한 datasource를 넘겨준다.
- Tasklet
  - 위에서 정의한 Tasklet을 스프링 빈에 등록
- Step
  - JobRepository와 PlatformTransactionManager를 파라미터로 받음
  - StepBuilder를 활용해 step이름, repository 설정
  - tasklet을 설정
- Job
  - Job 생성에서는 Step, Repository가 필요
  - incrementer는 job이 지속적으로 실행될때, joib의 유니크성을 구분할 수 있는 방법을 설정
  - RundIdIncrementer는 잡의 아이디를 실행할 때 지속적으로 증가시키면서 유니크한 잡을 실행
  - star(step)을 통해 job의 시작 포인트를 잡는다. 처음 시작하는 스텝은 우리가 파라미터로 받은 step을 등록

### 실행하기
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.4)

2024-10-11T13:58:17.075+09:00  INFO 37090 --- [batch] [           main] study.batch.BatchApplication             : Starting BatchApplication using Java 17.0.8.1 with PID 37090 (/Users/1004781/study/spring/batch/build/classes/java/main started by 1004781 in /Users/1004781/study/spring/batch)
2024-10-11T13:58:17.076+09:00  INFO 37090 --- [batch] [           main] study.batch.BatchApplication             : The following 1 profile is active: "local"
2024-10-11T13:58:17.321+09:00  WARN 37090 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'basicTaskJobConfiguration' of type [study.batch.BasicTaskJobConfiguration$$SpringCGLIB$$0] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T13:58:17.332+09:00  INFO 37090 --- [batch] [           main] o.s.j.d.e.EmbeddedDatabaseFactory        : Starting embedded database: url='jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'
2024-10-11T13:58:17.412+09:00  WARN 37090 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'dataSource' of type [org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory$EmbeddedDataSourceProxy] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T13:58:17.416+09:00  WARN 37090 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'transactionManager' of type [org.springframework.jdbc.datasource.DataSourceTransactionManager] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T13:58:17.421+09:00  WARN 37090 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'spring.batch-org.springframework.boot.autoconfigure.batch.BatchProperties' of type [org.springframework.boot.autoconfigure.batch.BatchProperties] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T13:58:17.424+09:00  WARN 37090 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration$SpringBootBatchConfiguration' of type [org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration$SpringBootBatchConfiguration] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). The currently created BeanPostProcessor [jobRegistryBeanPostProcessor] is declared through a non-static factory method on that class; consider declaring it as static instead.
2024-10-11T13:58:17.428+09:00  INFO 37090 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- After Properties Set() -----------------------
2024-10-11T13:58:17.468+09:00  INFO 37090 --- [batch] [           main] study.batch.BasicTaskJobConfiguration    : ------------------ Init myStep -----------------
2024-10-11T13:58:17.483+09:00  INFO 37090 --- [batch] [           main] study.batch.BasicTaskJobConfiguration    : ------------------ Init myJob -----------------
2024-10-11T13:58:17.549+09:00  INFO 37090 --- [batch] [           main] study.batch.BatchApplication             : Started BatchApplication in 0.622 seconds (process running for 0.917)
2024-10-11T13:58:17.550+09:00  INFO 37090 --- [batch] [           main] o.s.b.a.b.JobLauncherApplicationRunner   : Running default command line with: []
2024-10-11T13:58:17.569+09:00  INFO 37090 --- [batch] [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=myJob]] launched with the following parameters: [{'run.id':'{value=1, type=class java.lang.Long, identifying=true}'}]
2024-10-11T13:58:17.584+09:00  INFO 37090 --- [batch] [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [myStep]
2024-10-11T13:58:17.589+09:00  INFO 37090 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T13:58:17.589+09:00  INFO 37090 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=1}
2024-10-11T13:58:17.596+09:00  INFO 37090 --- [batch] [           main] o.s.batch.core.step.AbstractStep         : Step: [myStep] executed in 11ms
2024-10-11T13:58:17.599+09:00  INFO 37090 --- [batch] [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=myJob]] completed with the following parameters: [{'run.id':'{value=1, type=class java.lang.Long, identifying=true}'}] and the following status: [COMPLETED] in 23ms
2024-10-11T13:58:17.603+09:00  INFO 37090 --- [batch] [ionShutdownHook] o.s.j.d.e.EmbeddedDatabaseFactory        : Shutting down embedded database: url='jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false'

종료 코드 0(으)로 완료된 프로세스
```
- afterPropertySet(), Job, Step, Tasklet 순으로 실행됨

### Spring Batch 아키텍처
데보션 블로그에 상세한 설명이 되어 있음 (중복을 피하기 위해 링크) <br>
[상세 내용](https://devocean.sk.com/blog/techBoardDetail.do?ID=166690&boardType=techBlog&searchData=&searchDataMain=&page=&subIndex=&searchText=%EC%8A%A4%ED%94%84%EB%A7%81+%EB%B0%B0%EC%B9%98&techType=&searchDataSub=)

--- 
## 학습한 내용을 기반으로 여러 테스트 진행

### 1. RepeatStatus 동작 확인
**❓의문점 1❓** <br>
앞서 생성한 GreatingTask의 execute이 repeatable.CONTINUABLE을 리턴하면 어떤 결과가 일어날까?
```java
public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) throws Exception {
        logger.info("----------------------- Task Execute -----------------------");
        logger.info("GreetingTask : {}, {}", contribution, chunkContext);
        
        return RepeatStatus.CONTINUABLE;
    }
```
**❗결과 확인❗** <br>
계속 수행되니 하지 말자

**❓의문점 2❓** <br>
repeatable.continueIf 사용해보기 <br>
단순 테스트 작성 
- GreetingTask를 스프링 빈으로 등록하여 singleton instance가 되어 각 execute가 수행될 시 같은 인스턴스가 호출되게 됨.
- GreetingTask의 내부 변수를 두어 테스트 진행
- 실행될 때마다 count를 올려 10번 수행시 종료되는 Tasklet 작성
```java
public class GreetingTask implements Tasklet, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(GreetingTask.class);

    private int count = 0;
    
    @Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) throws Exception {
        logger.info("----------------------- Task Execute -----------------------");
        logger.info("GreetingTask : {}, {}", contribution, chunkContext);
        logger.info("Count : {}", ++count);

        return RepeatStatus.continueIf(count < 10);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("----------------------- After Properties Set() -----------------------");
    }
}
```

**❗결과 확인❗** <br>
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.4)

2024-10-11T14:24:51.016+09:00  INFO 42401 --- [batch] [           main] study.batch.BatchApplication             : Starting BatchApplication using Java 17.0.8.1 with PID 42401 (/Users/1004781/study/spring/batch/build/classes/java/main started by 1004781 in /Users/1004781/study/spring/batch)
2024-10-11T14:24:51.018+09:00  INFO 42401 --- [batch] [           main] study.batch.BatchApplication             : The following 1 profile is active: "local"
2024-10-11T14:24:51.301+09:00  WARN 42401 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'basicTaskJobConfiguration' of type [study.batch.BasicTaskJobConfiguration$$SpringCGLIB$$0] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T14:24:51.311+09:00  INFO 42401 --- [batch] [           main] o.s.j.d.e.EmbeddedDatabaseFactory        : Starting embedded database: url='jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false', username='sa'
2024-10-11T14:24:51.393+09:00  WARN 42401 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'dataSource' of type [org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory$EmbeddedDataSourceProxy] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T14:24:51.397+09:00  WARN 42401 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'transactionManager' of type [org.springframework.jdbc.datasource.DataSourceTransactionManager] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T14:24:51.402+09:00  WARN 42401 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'spring.batch-org.springframework.boot.autoconfigure.batch.BatchProperties' of type [org.springframework.boot.autoconfigure.batch.BatchProperties] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T14:24:51.405+09:00  WARN 42401 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration$SpringBootBatchConfiguration' of type [org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration$SpringBootBatchConfiguration] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). The currently created BeanPostProcessor [jobRegistryBeanPostProcessor] is declared through a non-static factory method on that class; consider declaring it as static instead.
2024-10-11T14:24:51.409+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- After Properties Set() -----------------------
2024-10-11T14:24:51.451+09:00  INFO 42401 --- [batch] [           main] study.batch.BasicTaskJobConfiguration    : ------------------ Init myStep -----------------
2024-10-11T14:24:51.467+09:00  INFO 42401 --- [batch] [           main] study.batch.BasicTaskJobConfiguration    : ------------------ Init myJob -----------------
2024-10-11T14:24:51.535+09:00  INFO 42401 --- [batch] [           main] study.batch.BatchApplication             : Started BatchApplication in 0.672 seconds (process running for 0.981)
2024-10-11T14:24:51.536+09:00  INFO 42401 --- [batch] [           main] o.s.b.a.b.JobLauncherApplicationRunner   : Running default command line with: []
2024-10-11T14:24:51.554+09:00  INFO 42401 --- [batch] [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=myJob]] launched with the following parameters: [{'run.id':'{value=1, type=class java.lang.Long, identifying=true}'}]
2024-10-11T14:24:51.565+09:00  INFO 42401 --- [batch] [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [myStep]
2024-10-11T14:24:51.569+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T14:24:51.569+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=1}
2024-10-11T14:24:51.573+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 1
2024-10-11T14:24:51.574+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T14:24:51.574+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=1}
2024-10-11T14:24:51.574+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 2
2024-10-11T14:24:51.575+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T14:24:51.575+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=1}
2024-10-11T14:24:51.575+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 3
2024-10-11T14:24:51.576+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T14:24:51.576+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=1}
2024-10-11T14:24:51.576+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 4
2024-10-11T14:24:51.577+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T14:24:51.578+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=1}
2024-10-11T14:24:51.578+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 5
2024-10-11T14:24:51.580+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T14:24:51.580+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=1}
2024-10-11T14:24:51.580+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 6
2024-10-11T14:24:51.581+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T14:24:51.581+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=1}
2024-10-11T14:24:51.581+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 7
2024-10-11T14:24:51.582+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T14:24:51.583+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=1}
2024-10-11T14:24:51.583+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 8
2024-10-11T14:24:51.584+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T14:24:51.584+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=1}
2024-10-11T14:24:51.584+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 9
2024-10-11T14:24:51.585+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T14:24:51.585+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=1}
2024-10-11T14:24:51.585+09:00  INFO 42401 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 10
2024-10-11T14:24:51.591+09:00  INFO 42401 --- [batch] [           main] o.s.batch.core.step.AbstractStep         : Step: [myStep] executed in 26ms
2024-10-11T14:24:51.594+09:00  INFO 42401 --- [batch] [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=myJob]] completed with the following parameters: [{'run.id':'{value=1, type=class java.lang.Long, identifying=true}'}] and the following status: [COMPLETED] in 34ms
2024-10-11T14:24:51.597+09:00  INFO 42401 --- [batch] [ionShutdownHook] o.s.j.d.e.EmbeddedDatabaseFactory        : Shutting down embedded database: url='jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false'

종료 코드 0(으)로 완료된 프로세스
```
- 10번 수행되고 멈추는 것을 확인
- 하나의 잡에서 수행되고 있는 것을 확인할 수 있음

### 2. 실제로 적재되는 데이터 확인하기 (DB 확인)
위에서 작성한 배치를 실행하고 DB(mysql) 데이터를 확인해보기 (DB 연결 코드는 생략) <br>
핵심 테이블만 상세하게 정리

**BATCH_JOB_INSTANCE**

| JOB\_INSTANCE\_ID | VERSION | JOB\_NAME | JOB\_KEY |
| :--- | :--- | :--- | :--- |
| 1 | 0 | myJob | 947cce338b790a4bb6cf8425e98bcf94 |
- Job_INSTANCE_ID : 인스턴스에 대한 유니크 ID
- VERSION: 버전정보 (레코드가 업데이트될 때마다 버전 업데이트)
- JOB_NAME: 설정한 Job name이 들어가 있다. (myJob)
- JobParameter를 직렬화한 값, 다른 잡과의 구분

**BATCH_JOB_EXECUTION** 

| JOB\_EXECUTION\_ID | VERSION | JOB\_INSTANCE\_ID | CREATE\_TIME | START\_TIME | END\_TIME | STATUS | EXIT\_CODE | EXIT\_MESSAGE | LAST\_UPDATED |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | 2 | 1 | 2024-10-11 05:38:40.156861 | 2024-10-11 05:38:40.182252 | 2024-10-11 05:38:40.275622 | COMPLETED | COMPLETED |  | 2024-10-11 05:38:40.276033 |
- JOB_EXECUTION_ID: 배치잡 실행 아이디
- Version: 버전정보 (레코드가 업데이트될 때마다 버전 업데이트)
- JOB_INSTANCE_ID: BATCH_JOB_INSTANCE 테이블의 기본키로 외래키. 하나의 인스턴스에는 여러개의 JOB EXECUTION이 있을 수 있음
- CREATE_TIME: execution이 생성된 시간
- END_TIME: execution이 종료된 시간
- STATUS: execution의 현재 상태를 문자열로 나타냄
- EXIT_CODE: execution의 종료 코드를 문자열로 나타냄
- EXIT_MESSAGE: job의 종료되는 경우 어떻게 종료되었는지를 나타냄
- LAST_UPDATED: execution이 마지막으로 지속된 시간을 나타내는 타임스탬프

**BATCH_JOB_EXECUTION_CONTEXT**

| JOB\_EXECUTION\_ID | SHORT\_CONTEXT | SERIALIZED\_CONTEXT |
| :--- | :--- | :--- |
| 1 | rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAABdAANYmF0Y2gudmVyc2lvbnQABTUuMS4yeA== | null |

- JOB_EXECUTION_ID: job execution 테이블에 대한 아이디로 외래키
- SHORT_CONTEXT: SERIALIZED_CONTEXT 의 문자로된 버젼
- SERIALIZED_CONTEXT: 직렬화된 전테 컨텍스트

**BATCH_JOB_EXECUTION_PARAMS**

| JOB\_EXECUTION\_ID | PARAMETER\_NAME | PARAMETER\_TYPE | PARAMETER\_VALUE | IDENTIFYING |
| :--- | :--- | :--- | :--- | :--- |
| 1 | run.id | java.lang.Long | 1 | Y |
- JOB_EXECUTION_ID: JOB 실행 ID. BATCH_JOB_EXECUTION으로 온 외래키
- PARAMETER_NAME: 파라미터 이름
- PARAMETER_TYPE: 파라미터 타입
- PARAMETER_VALUE: 파라미터 값
- IDENTIFYING: JobInstance의 유니크성을 위해 사용된 파라미터라면 true로 표기. id가 유니크성을 위해 사용되었기 때문에 true

**BATCH_JOB_EXECUTION_SEQ**

| ID | UNIQUE\_KEY |
| :--- | :--- |
| 1 | 0 |

**BATCH_JOB_SEQ**

| ID | UNIQUE\_KEY |
| :--- | :--- |
| 1 | 0 |

**BATCH_STEP_EXECUTION**

| STEP\_EXECUTION\_ID | VERSION | STEP\_NAME | JOB\_EXECUTION\_ID | CREATE\_TIME | START\_TIME | END\_TIME | STATUS | COMMIT\_COUNT | READ\_COUNT | FILTER\_COUNT | WRITE\_COUNT | READ\_SKIP\_COUNT | WRITE\_SKIP\_COUNT | PROCESS\_SKIP\_COUNT | ROLLBACK\_COUNT | EXIT\_CODE | EXIT\_MESSAGE | LAST\_UPDATED |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | 12 | myStep | 1 | 2024-10-11 05:38:40.198704 | 2024-10-11 05:38:40.207454 | 2024-10-11 05:38:40.268433 | COMPLETED | 10 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | COMPLETED |  | 2024-10-11 05:38:40.268993 |
- STEP_EXECUTION_ID: execution에 대한 유니크 아이디
- VERSION: 버전정보 (레코드가 업데이트될 때마다 버전 업데이트)
- STEP_NAME: execution에 등록된 step 이름 (설정한 myStep이 작성되어 있음)
- JOB_EXECUTION_ID: BATCH_JOB_EXECUTION 테이블에 대한 외래키
- START_TIME: execution이 시작된 시간
- END_TIME: execution이 종료된 시간
- COMMIT_COUNT: execution동안 트랜잭션 커밋된 카운트 (tasklet을 10번 수행)
- READ_COUNT: 이 스탭이 실행된 동안 읽어들인 아이템 수
- FILTER_COUNT: 이 스탭이 실행된 동안 필터된 아이템 수
- WRITE_COUNT: 이 스탭이 실행된 동안 쓰기된 아이템 수
- READ_SKIP_COUNT: 이 스탭이 실행된 동안 읽기기 스킵된 아이템 수
- WRITE_SKIP_COUNT: 이 스탭이 실행된 동안 쓰기가 스킵된 아이템 수
- PROCESS_SKIP_COUNT: 이 실행동안 롤백된 아이템
- EXIT_CODE: 이 실행동안 종료된 문자열
- EXIT_MESSAGE:: job이 종료되는 경우 어떻게 종료되었는지를 나타냄.
- LAST_UPDATED: execution이 마지막으로 지속된 시간을 나타내는 타임 스탬프

**BATCH_STEP_EXECUTION_SEQ**

| ID | UNIQUE\_KEY |
| :--- | :--- |
| 1 | 0 |


**BATCH_STEP_EXECUTION_CONTEXT**

| STEP\_EXECUTION\_ID | SHORT\_CONTEXT | SERIALIZED\_CONTEXT |
| :--- | :--- | :--- |
| 1 | rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAADdAARYmF0Y2gudGFza2xldFR5cGV0ACRzdHVkeS5iYXRjaC5qb2JzLnRhc2swMS5HcmVldGluZ1Rhc2t0AA1iYXRjaC52ZXJzaW9udAAFNS4xLjJ0AA5iYXRjaC5zdGVwVHlwZXQAN29yZy5zcHJpbmdmcmFtZXdvcmsuYmF0Y2guY29yZS5zdGVwLnRhc2tsZXQuVGFza2xldFN0ZXB4 | null |

**BATCH_STEP_EXECUTION_SEQ**

| ID | UNIQUE\_KEY |
| :--- | :--- |
| 1 | 0 |

### 3. Job이 모두 완료되기 전에 에러 발생 (DB 확인)
10번의 tasklet이 완료되기 전에 에러가 발생하면 어떻게 될까? <br>
7번째 tasklet에서 임의로 런타임에러 발생
```java
@Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) {
        logger.info("----------------------- Task Execute -----------------------");
        logger.info("GreetingTask : {}, {}", contribution, chunkContext);
        logger.info("Count : {}", ++count);

        if (count == 7) {
            throw new RuntimeException("Tasklet Error : count is 7");
        }

        return RepeatStatus.continueIf(count < 10);
    }
```

실행
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.4)

2024-10-11T16:03:57.724+09:00  INFO 62276 --- [batch] [           main] study.batch.BatchApplication             : Starting BatchApplication using Java 17.0.8.1 with PID 62276 (/Users/1004781/study/spring/batch/build/classes/java/main started by 1004781 in /Users/1004781/study/spring/batch)
2024-10-11T16:03:57.726+09:00  INFO 62276 --- [batch] [           main] study.batch.BatchApplication             : The following 1 profile is active: "local"
2024-10-11T16:03:58.033+09:00  WARN 62276 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.boot.autoconfigure.jdbc.DataSourceConfiguration$Hikari' of type [org.springframework.boot.autoconfigure.jdbc.DataSourceConfiguration$Hikari] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T16:03:58.047+09:00  WARN 62276 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'spring.datasource-org.springframework.boot.autoconfigure.jdbc.DataSourceProperties' of type [org.springframework.boot.autoconfigure.jdbc.DataSourceProperties] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T16:03:58.047+09:00  WARN 62276 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration$PooledDataSourceConfiguration' of type [org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration$PooledDataSourceConfiguration] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T16:03:58.048+09:00  WARN 62276 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'jdbcConnectionDetails' of type [org.springframework.boot.autoconfigure.jdbc.PropertiesJdbcConnectionDetails] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T16:03:58.058+09:00  WARN 62276 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'dataSource' of type [com.zaxxer.hikari.HikariDataSource] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T16:03:58.061+09:00  WARN 62276 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'basicTaskJobConfiguration' of type [study.batch.BasicTaskJobConfiguration$$SpringCGLIB$$0] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T16:03:58.063+09:00  WARN 62276 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'transactionManager' of type [org.springframework.jdbc.datasource.DataSourceTransactionManager] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T16:03:58.064+09:00  WARN 62276 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'spring.batch-org.springframework.boot.autoconfigure.batch.BatchProperties' of type [org.springframework.boot.autoconfigure.batch.BatchProperties] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). Is this bean getting eagerly injected into a currently created BeanPostProcessor [jobRegistryBeanPostProcessor]? Check the corresponding BeanPostProcessor declaration and its dependencies.
2024-10-11T16:03:58.067+09:00  WARN 62276 --- [batch] [           main] trationDelegate$BeanPostProcessorChecker : Bean 'org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration$SpringBootBatchConfiguration' of type [org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration$SpringBootBatchConfiguration] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying). The currently created BeanPostProcessor [jobRegistryBeanPostProcessor] is declared through a non-static factory method on that class; consider declaring it as static instead.
2024-10-11T16:03:58.071+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- After Properties Set() -----------------------
2024-10-11T16:03:58.078+09:00  INFO 62276 --- [batch] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2024-10-11T16:03:58.275+09:00  INFO 62276 --- [batch] [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@36cc9385
2024-10-11T16:03:58.276+09:00  INFO 62276 --- [batch] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2024-10-11T16:03:58.298+09:00  INFO 62276 --- [batch] [           main] study.batch.BasicTaskJobConfiguration    : ------------------ Init myStep -----------------
2024-10-11T16:03:58.310+09:00  INFO 62276 --- [batch] [           main] study.batch.BasicTaskJobConfiguration    : ------------------ Init myJob -----------------
2024-10-11T16:03:58.378+09:00  INFO 62276 --- [batch] [           main] study.batch.BatchApplication             : Started BatchApplication in 0.802 seconds (process running for 1.195)
2024-10-11T16:03:58.379+09:00  INFO 62276 --- [batch] [           main] o.s.b.a.b.JobLauncherApplicationRunner   : Running default command line with: []
2024-10-11T16:03:58.488+09:00  INFO 62276 --- [batch] [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=myJob]] launched with the following parameters: [{'run.id':'{value=5, type=class java.lang.Long, identifying=true}'}]
2024-10-11T16:03:58.515+09:00  INFO 62276 --- [batch] [           main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [myStep]
2024-10-11T16:03:58.525+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T16:03:58.525+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=5}
2024-10-11T16:03:58.533+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 1
2024-10-11T16:03:58.538+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T16:03:58.539+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=5}
2024-10-11T16:03:58.539+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 2
2024-10-11T16:03:58.543+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T16:03:58.543+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=5}
2024-10-11T16:03:58.543+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 3
2024-10-11T16:03:58.547+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T16:03:58.548+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=5}
2024-10-11T16:03:58.548+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 4
2024-10-11T16:03:58.551+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T16:03:58.551+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=5}
2024-10-11T16:03:58.551+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 5
2024-10-11T16:03:58.555+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T16:03:58.555+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=5}
2024-10-11T16:03:58.555+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 6
2024-10-11T16:03:58.559+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : ----------------------- Task Execute -----------------------
2024-10-11T16:03:58.559+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : GreetingTask : [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], ChunkContext: attributes=[], complete=false, stepContext=SynchronizedAttributeAccessor: [], stepExecutionContext={batch.version=5.1.2, batch.taskletType=study.batch.jobs.task01.GreetingTask, batch.stepType=org.springframework.batch.core.step.tasklet.TaskletStep}, jobExecutionContext={batch.version=5.1.2}, jobParameters={run.id=5}
2024-10-11T16:03:58.559+09:00  INFO 62276 --- [batch] [           main] study.batch.jobs.task01.GreetingTask     : Count : 7
2024-10-11T16:03:58.561+09:00 ERROR 62276 --- [batch] [           main] o.s.batch.core.step.AbstractStep         : Encountered an error executing step myStep in job myJob

java.lang.RuntimeException: Tasklet Error : count is 7
	at study.batch.jobs.task01.GreetingTask.execute(GreetingTask.java:24) ~[main/:na]
	at org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:388) ~[spring-batch-core-5.1.2.jar:5.1.2]
	at org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction(TaskletStep.java:312) ~[spring-batch-core-5.1.2.jar:5.1.2]
	at org.springframework.transaction.support.TransactionTemplate.execute(TransactionTemplate.java:140) ~[spring-tx-6.1.13.jar:6.1.13]
	at org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext(TaskletStep.java:255) ~[spring-batch-core-5.1.2.jar:5.1.2]
	at org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration(StepContextRepeatCallback.java:82) ~[spring-batch-core-5.1.2.jar:5.1.2]
	at org.springframework.batch.repeat.support.RepeatTemplate.getNextResult(RepeatTemplate.java:369) ~[spring-batch-infrastructure-5.1.2.jar:5.1.2]
	at org.springframework.batch.repeat.support.RepeatTemplate.executeInternal(RepeatTemplate.java:206) ~[spring-batch-infrastructure-5.1.2.jar:5.1.2]
	at org.springframework.batch.repeat.support.RepeatTemplate.iterate(RepeatTemplate.java:140) ~[spring-batch-infrastructure-5.1.2.jar:5.1.2]
	at org.springframework.batch.core.step.tasklet.TaskletStep.doExecute(TaskletStep.java:240) ~[spring-batch-core-5.1.2.jar:5.1.2]
	at org.springframework.batch.core.step.AbstractStep.execute(AbstractStep.java:229) ~[spring-batch-core-5.1.2.jar:5.1.2]
	at org.springframework.batch.core.job.SimpleStepHandler.handleStep(SimpleStepHandler.java:153) ~[spring-batch-core-5.1.2.jar:5.1.2]
	at org.springframework.batch.core.job.AbstractJob.handleStep(AbstractJob.java:418) ~[spring-batch-core-5.1.2.jar:5.1.2]
	at org.springframework.batch.core.job.SimpleJob.doExecute(SimpleJob.java:132) ~[spring-batch-core-5.1.2.jar:5.1.2]
	at org.springframework.batch.core.job.AbstractJob.execute(AbstractJob.java:317) ~[spring-batch-core-5.1.2.jar:5.1.2]
	at org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run(SimpleJobLauncher.java:157) ~[spring-batch-core-5.1.2.jar:5.1.2]
	at org.springframework.core.task.SyncTaskExecutor.execute(SyncTaskExecutor.java:50) ~[spring-core-6.1.13.jar:6.1.13]
	at org.springframework.batch.core.launch.support.SimpleJobLauncher.run(SimpleJobLauncher.java:148) ~[spring-batch-core-5.1.2.jar:5.1.2]
	at org.springframework.batch.core.launch.support.TaskExecutorJobLauncher.run(TaskExecutorJobLauncher.java:59) ~[spring-batch-core-5.1.2.jar:5.1.2]
	at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.execute(JobLauncherApplicationRunner.java:210) ~[spring-boot-autoconfigure-3.3.4.jar:3.3.4]
	at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.executeLocalJobs(JobLauncherApplicationRunner.java:194) ~[spring-boot-autoconfigure-3.3.4.jar:3.3.4]
	at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.launchJobFromProperties(JobLauncherApplicationRunner.java:174) ~[spring-boot-autoconfigure-3.3.4.jar:3.3.4]
	at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.run(JobLauncherApplicationRunner.java:169) ~[spring-boot-autoconfigure-3.3.4.jar:3.3.4]
	at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.run(JobLauncherApplicationRunner.java:164) ~[spring-boot-autoconfigure-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.lambda$callRunner$4(SpringApplication.java:786) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.util.function.ThrowingConsumer$1.acceptWithException(ThrowingConsumer.java:83) ~[spring-core-6.1.13.jar:6.1.13]
	at org.springframework.util.function.ThrowingConsumer.accept(ThrowingConsumer.java:60) ~[spring-core-6.1.13.jar:6.1.13]
	at org.springframework.util.function.ThrowingConsumer$1.accept(ThrowingConsumer.java:88) ~[spring-core-6.1.13.jar:6.1.13]
	at org.springframework.boot.SpringApplication.callRunner(SpringApplication.java:798) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.callRunner(SpringApplication.java:786) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.lambda$callRunners$3(SpringApplication.java:774) ~[spring-boot-3.3.4.jar:3.3.4]
	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:183) ~[na:na]
	at java.base/java.util.stream.SortedOps$SizedRefSortingSink.end(SortedOps.java:357) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:510) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499) ~[na:na]
	at java.base/java.util.stream.ForEachOps$ForEachOp.evaluateSequential(ForEachOps.java:150) ~[na:na]
	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateSequential(ForEachOps.java:173) ~[na:na]
	at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234) ~[na:na]
	at java.base/java.util.stream.ReferencePipeline.forEach(ReferencePipeline.java:596) ~[na:na]
	at org.springframework.boot.SpringApplication.callRunners(SpringApplication.java:774) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:342) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1363) ~[spring-boot-3.3.4.jar:3.3.4]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1352) ~[spring-boot-3.3.4.jar:3.3.4]
	at study.batch.BatchApplication.main(BatchApplication.java:10) ~[main/:na]

2024-10-11T16:03:58.564+09:00  INFO 62276 --- [batch] [           main] o.s.batch.core.step.AbstractStep         : Step: [myStep] executed in 49ms
2024-10-11T16:03:58.577+09:00  INFO 62276 --- [batch] [           main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=myJob]] completed with the following parameters: [{'run.id':'{value=5, type=class java.lang.Long, identifying=true}'}] and the following status: [FAILED] in 79ms
2024-10-11T16:03:58.582+09:00  INFO 62276 --- [batch] [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown initiated...
2024-10-11T16:03:58.622+09:00  INFO 62276 --- [batch] [ionShutdownHook] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.

종료 코드 0(으)로 완료된 프로세스
```

테이블 확인

**BATCH_JOB_EXECUTION**

| JOB\_EXECUTION\_ID | VERSION | JOB\_INSTANCE\_ID | CREATE\_TIME | START\_TIME | END\_TIME | STATUS | EXIT\_CODE | EXIT\_MESSAGE | LAST\_UPDATED |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 5 | 2 | 5 | 2024-10-11 07:03:58.474087 | 2024-10-11 07:03:58.493640 | 2024-10-11 07:03:58.572671 | FAILED | FAILED | java.lang.RuntimeException: Tasklet Error : count is 7<br/>at study.batch.jobs.task01.GreetingTask.execute\(GreetingTask.java:24\)<br/>at org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction\(TaskletStep.java:388\)<br/>at org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction\(TaskletStep.java:312\)<br/>at org.springframework.transaction.support.TransactionTemplate.execute\(TransactionTemplate.java:140\)<br/>at org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext\(TaskletStep.java:255\)<br/>at org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration\(StepContextRepeatCallback.java:82\)<br/>at org.springframework.batch.repeat.support.RepeatTemplate.getNextResult\(RepeatTemplate.java:369\)<br/>at org.springframework.batch.repeat.support.RepeatTemplate.executeInternal\(RepeatTemplate.java:206\)<br/>at org.springframework.batch.repeat.support.RepeatTemplate.iterate\(RepeatTemplate.java:140\)<br/>at org.springframework.batch.core.step.tasklet.TaskletStep.doExecute\(TaskletStep.java:240\)<br/>at org.springframework.batch.core.step.AbstractStep.execute\(AbstractStep.java:229\)<br/>at org.springframework.batch.core.job.SimpleStepHandler.handleStep\(SimpleStepHandler.java:153\)<br/>at org.springframework.batch.core.job.AbstractJob.handleStep\(AbstractJob.java:418\)<br/>at org.springframework.batch.core.job.SimpleJob.doExecute\(SimpleJob.java:132\)<br/>at org.springframework.batch.core.job.AbstractJob.execute\(AbstractJob.java:317\)<br/>at org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run\(SimpleJobLauncher.java:157\)<br/>at org.springframework.core.task.SyncTaskExecutor.execute\(SyncTaskExecutor.java:50\)<br/>at org.springframework.batch.core.launch.support.SimpleJobLauncher.run\(SimpleJobLauncher.java:148\)<br/>at org.springframework.batch.core.launch.support.TaskExecutorJobLauncher.run\(TaskExecutorJobLauncher.java:59\)<br/>at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.execute\(JobLauncherApplicationRunner.java:210\)<br/>at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.executeLocalJobs\(JobLauncherApplicationRunner.java:194\)<br/>at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.launchJobFromProperties\(JobLauncherApplicationRunner.java:174\)<br/>at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.run\(JobLauncherApplicationRunner.java:169\)<br/>at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRu | 2024-10-11 07:03:58.572976 |
- FAILED 처리가 되고 에러 로그가 잘 적재됨

**BATCH_STEP_EXECUTION**

| STEP\_EXECUTION\_ID | VERSION | STEP\_NAME | JOB\_EXECUTION\_ID | CREATE\_TIME | START\_TIME | END\_TIME | STATUS | COMMIT\_COUNT | READ\_COUNT | FILTER\_COUNT | WRITE\_COUNT | READ\_SKIP\_COUNT | WRITE\_SKIP\_COUNT | PROCESS\_SKIP\_COUNT | ROLLBACK\_COUNT | EXIT\_CODE | EXIT\_MESSAGE | LAST\_UPDATED |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 5 | 8 | myStep | 5 | 2024-10-11 07:03:58.505377 | 2024-10-11 07:03:58.515216 | 2024-10-11 07:03:58.564828 | FAILED | 6 | 0 | 0 | 0 | 0 | 0 | 0 | 1 | FAILED | java.lang.RuntimeException: Tasklet Error : count is 7<br/>at study.batch.jobs.task01.GreetingTask.execute\(GreetingTask.java:24\)<br/>at org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction\(TaskletStep.java:388\)<br/>at org.springframework.batch.core.step.tasklet.TaskletStep$ChunkTransactionCallback.doInTransaction\(TaskletStep.java:312\)<br/>at org.springframework.transaction.support.TransactionTemplate.execute\(TransactionTemplate.java:140\)<br/>at org.springframework.batch.core.step.tasklet.TaskletStep$2.doInChunkContext\(TaskletStep.java:255\)<br/>at org.springframework.batch.core.scope.context.StepContextRepeatCallback.doInIteration\(StepContextRepeatCallback.java:82\)<br/>at org.springframework.batch.repeat.support.RepeatTemplate.getNextResult\(RepeatTemplate.java:369\)<br/>at org.springframework.batch.repeat.support.RepeatTemplate.executeInternal\(RepeatTemplate.java:206\)<br/>at org.springframework.batch.repeat.support.RepeatTemplate.iterate\(RepeatTemplate.java:140\)<br/>at org.springframework.batch.core.step.tasklet.TaskletStep.doExecute\(TaskletStep.java:240\)<br/>at org.springframework.batch.core.step.AbstractStep.execute\(AbstractStep.java:229\)<br/>at org.springframework.batch.core.job.SimpleStepHandler.handleStep\(SimpleStepHandler.java:153\)<br/>at org.springframework.batch.core.job.AbstractJob.handleStep\(AbstractJob.java:418\)<br/>at org.springframework.batch.core.job.SimpleJob.doExecute\(SimpleJob.java:132\)<br/>at org.springframework.batch.core.job.AbstractJob.execute\(AbstractJob.java:317\)<br/>at org.springframework.batch.core.launch.support.SimpleJobLauncher$1.run\(SimpleJobLauncher.java:157\)<br/>at org.springframework.core.task.SyncTaskExecutor.execute\(SyncTaskExecutor.java:50\)<br/>at org.springframework.batch.core.launch.support.SimpleJobLauncher.run\(SimpleJobLauncher.java:148\)<br/>at org.springframework.batch.core.launch.support.TaskExecutorJobLauncher.run\(TaskExecutorJobLauncher.java:59\)<br/>at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.execute\(JobLauncherApplicationRunner.java:210\)<br/>at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.executeLocalJobs\(JobLauncherApplicationRunner.java:194\)<br/>at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.launchJobFromProperties\(JobLauncherApplicationRunner.java:174\)<br/>at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner.run\(JobLauncherApplicationRunner.java:169\)<br/>at org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRu | 2024-10-11 07:03:58.565303 |
- FAILED 처리가 되고 에러 로그가 잘 적재됨
- 6개의 tasklet은 커밋이 되고, 1개의 tasklet은 실패처리가 됨

**❓의문점❓** <br>
실행되지 않은 3개의 작업은 어떻게 처리를 해야할까?


### 2주차 WrapUp

---
Tasklet 실행 후 테이블 분석 <br>
처리의 흐름과 데이터의 흐름 확인

ref 
- https://devocean.sk.com/blog/techBoardDetail.do?ID=166690
- https://devocean.sk.com/blog/techBoardDetail.do?ID=166164