package formflow.library.scheduled_tasks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.system.ApplicationTemp;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;

@Slf4j
@Configuration
@EnableScheduling
public class TempFileDeletionScheduler {

    @Scheduled(fixedRate = 5000)
    public void deleteTempFiles() {
        File tempDirectory = new ApplicationTemp().getDir();
        log.info("Deleting temp files!");
    }
}
