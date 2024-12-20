package study.batch.week3;

import lombok.extern.java.Log;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.ConcurrentHashMap;


@Log
@Configuration
public class PlayerJobConfiguration {
    public static final String ENCODING = "UTF-8";
    public static final String TOTAL_PLAYERS = "TOTAL_PLAYERS";
    public static final String TOTAL_AGES = "TOTAL_AGES";

    private final ConcurrentHashMap<String, Integer> aggregateInfos = new ConcurrentHashMap<>();

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
    public FlatFileItemWriter<Player> playerFlatFileItemWriter() {
        return new FlatFileItemWriterBuilder<Player>()
                .name("customerFlatFileItemWriter")
                .resource(new FileSystemResource("./output/week3/player_new.csv"))
                .encoding(ENCODING)
                .delimited()
                .names("No", "Name", "Age")
                .append(false)
                .headerCallback(writer -> writer.write("No,Name,Age"))
                .footerCallback(writer -> {
                    writer.write("총 플레이어 수: " + aggregateInfos.get(TOTAL_PLAYERS));
                    writer.write(System.lineSeparator());
                    writer.write("총 나이: " + aggregateInfos.get(TOTAL_AGES));
                })
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
                    aggregateInfos.putIfAbsent(TOTAL_PLAYERS, 0);
                    aggregateInfos.putIfAbsent(TOTAL_AGES, 0);

                    aggregateInfos.put(TOTAL_PLAYERS, aggregateInfos.get(TOTAL_PLAYERS) + 1);
                    aggregateInfos.put(TOTAL_AGES, aggregateInfos.get(TOTAL_AGES) + player.getAge());
                    return player;
                })
                .writer(playerFlatFileItemWriter())
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
