package study.batch.week3;

import lombok.extern.java.Log;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;


@Log
@Configuration
public class PlayerJobConfiguration {

    @Bean
    public FieldSetMapper<Player> playerFieldSetMapper() {
//        return new PlayerFieldSetMapper();
        return new PlayerMapper();
    }

    @Bean
    public DelimitedLineTokenizer delimitedLineTokenizer() {
        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
        delimitedLineTokenizer.setNames("No", "Name", "Age");
        return delimitedLineTokenizer;
    }

    @Bean
    public DefaultLineMapper<Player> playerLineMapper() {
        DefaultLineMapper<Player> lineMapper = new DefaultLineMapper<> ();
        lineMapper.setLineTokenizer(delimitedLineTokenizer());
        lineMapper.setFieldSetMapper(playerFieldSetMapper());

        return lineMapper;
    }

    @Bean
    public FlatFileItemReader<Player> playerFlatFileItemReader() {
        return new FlatFileItemReaderBuilder<Player>()
                .name("playerFlatFileItemReader")
                .resource(new FileSystemResource("src/main/resources/week3/players.csv"))
                .linesToSkip(1) // 표의 title skip
                .lineMapper(playerLineMapper())
                .build();
    }

    @Bean
    public Step playerStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        log.info("------------------ Init PlayerStep -----------------");
        return new StepBuilder("flatPlayerStep", jobRepository)
                .<Player, Player>chunk(10, transactionManager)
                .reader(playerFlatFileItemReader())
                .processor(player -> {
                    log.info("------------------ Processor Execute ------------------");
                    player.setAge(player.getAge()+1);
                    return player;
                })
                .writer(chunk -> {
                    log.info("------------------ WRITE Execute ------------------");
                    chunk.getItems().forEach(item -> log.info(item.toString()));
                })
                .build();
    }

    @Bean
    public Job playerJob(Step playerStep, JobRepository jobRepository) {
        log.info("------------------ Init PlayerJob -----------------");
        return new JobBuilder("flatPlayerJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(playerStep)
                .build();
    }
}
