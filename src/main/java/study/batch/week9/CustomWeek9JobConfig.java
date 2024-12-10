package study.batch.week9;

import jakarta.persistence.EntityManagerFactory;
import lombok.extern.java.Log;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import study.batch.common.Customer;
import study.batch.common.QCustomer;

import javax.sql.DataSource;

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
