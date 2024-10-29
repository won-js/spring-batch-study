package study.batch.week2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;

public class GreetingTask implements Tasklet, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(GreetingTask.class);

    @Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) {
        logger.info("----------------------- Task Execute -----------------------");
        logger.info("GreetingTask : {}, {}", contribution, chunkContext);

        return RepeatStatus.FINISHED;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("----------------------- After Properties Set() -----------------------");
    }
}
