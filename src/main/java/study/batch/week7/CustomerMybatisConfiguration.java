package study.batch.week7;

import lombok.extern.java.Log;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.MyBatisPagingItemReader;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.mybatis.spring.batch.builder.MyBatisPagingItemReaderBuilder;
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

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

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
