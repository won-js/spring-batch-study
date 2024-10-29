package study.batch.week2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

public class GreetingTaskCounter extends GreetingTask {
    private static final Logger logger = LoggerFactory.getLogger(GreetingTaskCounter.class);

    private int count = 0;

    @Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) {
        logger.info("----------------------- Task Execute -----------------------");
        logger.info("GreetingTask : {}, {}", contribution, chunkContext);
        logger.info("Count : {}", ++count);

        return RepeatStatus.continueIf(count < 10);
    }
}
