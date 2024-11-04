package study.batch.week5;

import lombok.extern.java.Log;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.*;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;
import study.batch.common.Customer;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Log
@Configuration
public class CustomerJdbcJobConfiguration {
    private static final String CUSTOMER_JDBC_JOB = "customerJdbcJob";
    private static final String CUSTOMER_JDBC_STEP = "customerJdbcStep";
    private static final String CUSTOMER_JDBC_READER = "customerJdbcReader";
    private static final int CHUNK_SIZE = 10;

    @Autowired
    DataSource dataSource;

    @Bean
    public PagingQueryProvider queryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("ID, NAME, AGE, GENDER");
        queryProvider.setFromClause("from CUSTOMER");
        queryProvider.setWhereClause("where AGE >= :age");

        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("id", Order.DESCENDING);

        queryProvider.setSortKeys(sortKeys);

        return queryProvider.getObject();
    }

    @Bean
    public JdbcPagingItemReader<Customer> customerJdbcPagingItemReader() throws Exception {
        Map<String, Object> parameterValue = new HashMap<>();
        parameterValue.put("age",20);

        return new JdbcPagingItemReaderBuilder<Customer>()
                .name(CUSTOMER_JDBC_READER)
                .fetchSize(CHUNK_SIZE)
                .dataSource(dataSource)
                .rowMapper(new BeanPropertyRowMapper<>(Customer.class))
                .parameterValues(parameterValue)
                .queryProvider(queryProvider())
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Customer> customerJdbcBatchItemWriter() {
        return new JdbcBatchItemWriterBuilder<Customer>()
                .dataSource(dataSource)
                .sql("UPDATE CUSTOMER SET GRADE = :grade where ID = :id")
                .itemSqlParameterSourceProvider(BeanPropertySqlParameterSource::new)
                .build();
    }

    @Bean
    public Step customerJdbcPagingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) throws Exception {
        log.info("------------------ Init customerJdbcPagingStep -----------------");

        return new StepBuilder(CUSTOMER_JDBC_STEP, jobRepository)
                .<Customer, Customer>chunk(CHUNK_SIZE, transactionManager)
                .reader(customerJdbcPagingItemReader())
                .processor(customer -> {
                    customer.assignGroup();
                    return customer;
                })
                .writer(customerJdbcBatchItemWriter())
                .build();
    }

    @Bean
    public Job customerJdbcPaginJob(Step customerJdbcPagingStep, JobRepository jobRepository) {
        log.info("------------------ Init customerJdbcPagingJob -----------------");
        return new JobBuilder(CUSTOMER_JDBC_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(customerJdbcPagingStep)
                .build();

    }
}
