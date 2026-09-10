package formflow.library.repository;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies that {@link SubmissionRepositoryService#generateAndSetUniqueShortCode} correctly handles a genuine
 * short-code collision at save time (two callers both pass the {@code existsByShortCode} pre-check for the same
 * code, and only one of the two inserts can win, since the {@code short_code} column is uniquely constrained) -
 * rather than letting the loser's save throw an unhandled {@code DataIntegrityViolationException}.
 * <p>
 * The code space is shrunk to 10 possible codes ("0"-"9") via {@code @TestPropertySource}, and 9 of them are
 * pre-occupied, leaving exactly one code every concurrent caller can possibly land on - guaranteeing genuine
 * contention on that single code rather than relying on chance.
 */
@ActiveProfiles("test")
@SpringBootTest(properties = {"form-flow.path=flows-config/test-flow.yaml"})
@TestPropertySource(properties = {
        "form-flow.short-code.short-code-configs.testFlow.code-length=1",
        "form-flow.short-code.short-code-configs.testFlow.code-type=numeric",
        "form-flow.short-code.short-code-configs.testFlow.uppercase=false",
        "form-flow.short-code.short-code-configs.testFlow.prefix=",
        "form-flow.short-code.short-code-configs.testFlow.suffix="
})
class ShortCodeConcurrencyTest {

    private static final int THREAD_COUNT = 2;

    @Autowired
    private SubmissionRepositoryService submissionRepositoryService;

    @Test
    void concurrentCallersContendingForTheOnlyRemainingCodeAllSucceedOrGracefullyGiveUp() throws Exception {
        // occupy 9 of the 10 possible single-digit codes, leaving "9" as the only one anyone can land on.
        for (int i = 0; i < 9; i++) {
            Submission occupied = new Submission();
            occupied.setFlow("testFlow");
            occupied.setShortCode(String.valueOf(i));
            submissionRepositoryService.save(occupied);
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Submission>> futures = new ArrayList<>();

        try {
            IntStream.range(0, THREAD_COUNT).forEach(i -> futures.add(executor.submit(() -> {
                Submission submission = new Submission();
                submission.setFlow("testFlow");
                submission = submissionRepositoryService.save(submission);
                startLatch.await();
                submissionRepositoryService.generateAndSetUniqueShortCode(submission);
                return submission;
            })));

            startLatch.countDown();

            List<Submission> results = Collections.synchronizedList(new ArrayList<>());
            for (Future<Submission> future : futures) {
                // A hung/uncaught exception here (e.g. a poisoned transaction after the collision) would fail
                // this via timeout or a rethrown exception - that's exactly what this test guards against.
                results.add(future.get(30, TimeUnit.SECONDS));
            }

            List<Submission> withCode = results.stream().filter(s -> s.getShortCode() != null).toList();
            List<Submission> withoutCode = results.stream().filter(s -> s.getShortCode() == null).toList();

            assertThat(withCode).as("exactly one caller should have won the only remaining code").hasSize(1);
            assertThat(withCode.get(0).getShortCode()).isEqualTo("9");
            assertThat(withoutCode).as("the losers should give up gracefully rather than throw")
                    .hasSize(THREAD_COUNT - 1);
        } finally {
            executor.shutdownNow();
        }
    }
}
