package study.batch.week8;

import lombok.extern.java.Log;
import org.mybatis.spring.batch.MyBatisPagingItemReader;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import study.batch.common.Customer;

import java.util.List;

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
