package formflow.library.scheduled_tasks;

import org.junit.jupiter.api.Test;

class TempFileDeletionSchedulerTest {

    TempFileDeletionScheduler tempFileDeletionScheduler = new TempFileDeletionScheduler();

    @Test
    void deleteTempFiles() {
        // Create 2 temp files and place them in the temp file directory
        // Call deleteTempFiles()
        // Assert that the 2 temp files are no longer in the temp file directory
        tempFileDeletionScheduler.deleteTempFiles();
    }
}