# 4주차 SPRING BATCH STUDY

## FlatFileItemReader 개요

---
- FlatFileItemReader는 Spring Batch에서 제공하는 기본적인 ItemReader로, 텍스트 파일로부터 데이터를 읽습니다.
- 고정 길이, 구분자 기반, 멀티라인 등 다양한 형식의 텍스트 파일을 지원하며, 다음과 같은 장점을 가짐
  - 간단하고 효율적 구현: 설정 및 사용이 간편하며, 대규모 데이터 처리에도 효율적
  - 다양한 텍스트 파일 형식 지원: 고정 길이, 구분자 기반, 멀티라인 등 다양한 형식의 텍스트 파일을 읽을 수 있음
  - 확장 가능성: 토크나이저, 필터 등을 통해 기능을 확장할 수 있음
  - 사용처: 고정 길이, 구분자 기반, 멀티라인 등 다양한 형식의 텍스트 파일 데이터 처리

## FlatFileItemReader 주요 구성 요소

---
- Resource: 읽을 텍스트 파일을 지정
- LineMapper: 텍스트 파일의 각 라인을 Item으로 변환하는 역할
- LineTokenizer: 텍스트 파일의 각 라인을 토큰으로 분리
- FieldSetMapper: 토큰을 Item의 속성에 매핑하는 역할
- SkippableLineMapper: 오류 발생 시 해당 라인을 건너뛸 수 있도록 설정
- LineCallbackHandler: 라인별로 처리를 수행할 수 있도록 작업
- ReadListener: 읽기 시작, 종료, 오류 발생 등의 이벤트를 처리

## FlatFileItemWriter 개요

---
- FlatFileItemWriter는 Spring Batch에서 제공하는 ItemWriter 인터페이스를 구현하는 클래스
- 데이터를 텍스트 파일로 출력하는데 사용됨


## 구성요소

---
- Resource: 출력 파일 경로 지정
- LineAggregator: Item을 문자열로 변환하는 역할
- HeaderCallback: 출력 파일 헤더를 작성하는 역할
- FooterCallback: 출력 파일 풋터를 작성하는 역할
- Delimiter: 항복 사이 구분자 지정
- AppendMode: 기존 파일에 추가할지 여부를 지정

## 테스트 샘플
```java
package study.batch.week4;

import lombok.extern.java.Log;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.ConcurrentHashMap;

@Log
@Configuration
public class CustomerJobConfiguration {
    public static final int CHUNK_SIZE = 100;
    public static final String ENCODING = "UTF-8";
    public static final String FLAT_FILE_CHUNK_JOB = "customerFlatFileJob";
    public static final String TOTAL_CUSTOMERS = "TOTAL_CUSTOMERS";
    public static final String TOTAL_AGES = "TOTAL_AGES";

    private final ConcurrentHashMap<String, Integer> aggregateInfos = new ConcurrentHashMap<>();


    @Bean
    public FlatFileItemReader<Customer> customerFlatFileItemReader() {
        return new FlatFileItemReaderBuilder<Customer>()
                .name("customerFlatFileItemReader")
                .resource(new FileSystemResource("src/main/resources/week4/customers.csv"))
                .encoding(ENCODING)
                .linesToSkip(1)
                .delimited().delimiter(",")
                .names("name", "age", "gender")
                .targetType(Customer.class)
                .build();
    }

    @Bean
    public ItemProcessor<Customer, Customer> customerItemProcessor() {
        return item -> {
            aggregateInfos.putIfAbsent(TOTAL_CUSTOMERS, 0);
            aggregateInfos.putIfAbsent(TOTAL_AGES, 0);

            aggregateInfos.put(TOTAL_CUSTOMERS, aggregateInfos.get(TOTAL_CUSTOMERS) + 1);
            aggregateInfos.put(TOTAL_AGES, aggregateInfos.get(TOTAL_AGES) + item.getAge());
            return item;
        };
    }

    @Bean
    public FlatFileItemWriter<Customer> customerFlatFileItemWriter() {
        return new FlatFileItemWriterBuilder<Customer>()
                .name("customerFlatFileItemWriter")
                .resource(new FileSystemResource("./output/week4/customer_new.csv"))
                .encoding(ENCODING)
                .delimited().delimiter("\t")
                .names("Name", "Age", "Gender")
                .append(false)
                .lineAggregator(item ->
                    item.getName() + "," + item.getAge()
                )
                .headerCallback(writer -> writer.write("ID,AGE"))
                .footerCallback(writer -> {
                    writer.write("총 고객 수: " + aggregateInfos.get(TOTAL_CUSTOMERS));
                    writer.write(System.lineSeparator());
                    writer.write("총 나이: " + aggregateInfos.get(TOTAL_AGES));
                })
                .build();
    }

    @Bean
    public Step customerFlatFileStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("------------------ Init flatFileStep -----------------");

        return new StepBuilder("customerFlatFileStep", jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(customerFlatFileItemReader())
                .processor(customerItemProcessor())
                .writer(customerFlatFileItemWriter())
                .build();
    }

    @Bean
    public Job flatFileJob(Step customerFlatFileStep, JobRepository jobRepository) {
        log.info("------------------ Init flatFileJob -----------------");
        return new JobBuilder(FLAT_FILE_CHUNK_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerFlatFileStep)
                .build();
    }
}
```


### ref
- https://devocean.sk.com/blog/techBoardDetail.do?ID=166828&boardType=techBlog&searchData=&searchDataMain=&page=&subIndex=&searchText=spring+batch&techType=&searchDataSub=
