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
