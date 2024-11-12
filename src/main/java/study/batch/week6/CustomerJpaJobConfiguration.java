package study.batch.week6;

import jakarta.persistence.EntityManagerFactory;
import lombok.extern.java.Log;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import study.batch.common.Customer;

import java.util.Collections;

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
    public Step customerJpaPagingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager){
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
