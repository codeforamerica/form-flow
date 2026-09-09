package formflow.library;

import static org.assertj.core.api.Assertions.assertThat;

import formflow.library.data.Submission;
import formflow.library.data.SubmissionRepositoryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that {@link FormFlowController#findOrCreateSubmission} can't create duplicate {@link Submission}s for the same
 * session/flow when called concurrently - by multiple threads sharing one session object, and by multiple distinct
 * {@link jakarta.servlet.http.HttpSession} objects that represent the same logical session (the way a Spring Session-backed
 * deployment hands out a new session wrapper instance per request).
 */
@ActiveProfiles("test")
@SpringBootTest(properties = {"form-flow.path=/flows-config/test-flow.yaml"})
class FormFlowControllerConcurrencyTest {

    private static final int THREAD_COUNT = 10;

    @Autowired
    private ScreenController screenController;

    @Autowired
    private SubmissionRepositoryService submissionRepositoryService;

    @PersistenceContext
    private EntityManager entityManager;

    // Has to be a real configured flow: the encryption service resolves an input class from the
    // flow name, so an arbitrary made-up flow name fails before locking even comes into play.
    private static final String FLOW = "testFlow";

    @Test
    void concurrentRequestsSharingOneSessionObjectCreateOnlyOneSubmission() throws Exception {
        MockHttpSession session = new MockHttpSession();
        long countBefore = countSubmissionsForFlow();

        List<Submission> results = runConcurrently(THREAD_COUNT, () -> screenController.findOrCreateSubmission(session, FLOW));

        assertOnlyOneSubmissionWasCreated(results, countBefore);
    }

    @Test
    void concurrentRequestsOnDifferentSessionObjectsWithTheSameIdCreateOnlyOneSubmission() throws Exception {
        String sharedSessionId = "shared-session-" + UUID.randomUUID();
        Map<String, Object> sharedAttributeStore = new ConcurrentHashMap<>();
        long countBefore = countSubmissionsForFlow();

        List<Submission> results = runConcurrently(THREAD_COUNT, () -> {
            // A fresh HttpSession-like object per call, exactly like Spring Session hands a new
            // wrapper instance to every request even though it's backed by the same underlying,
            // shared (here: in-memory-but-shared) session storage.
            SharedStoreMockHttpSession session = new SharedStoreMockHttpSession(sharedSessionId, sharedAttributeStore);
            return screenController.findOrCreateSubmission(session, FLOW);
        });

        assertOnlyOneSubmissionWasCreated(results, countBefore);
    }

    private void assertOnlyOneSubmissionWasCreated(List<Submission> results, long countBefore) {
        assertThat(results).hasSize(THREAD_COUNT);
        assertThat(results).allSatisfy(submission -> assertThat(submission.getId()).isNotNull());

        Set<UUID> distinctIds = results.stream().map(Submission::getId).collect(Collectors.toSet());
        assertThat(distinctIds).as("every concurrent caller should have been handed back the same submission").hasSize(1);

        long countAfter = countSubmissionsForFlow();
        assertThat(countAfter - countBefore)
                .as("exactly one new submission row should have been persisted for this flow")
                .isEqualTo(1L);
    }

    private long countSubmissionsForFlow() {
        return (Long) entityManager
                .createQuery("SELECT COUNT(s) FROM Submission s WHERE s.flow = :flow")
                .setParameter("flow", FLOW)
                .getSingleResult();
    }

    private List<Submission> runConcurrently(int threadCount, java.util.function.Supplier<Submission> action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Submission>> futures = new ArrayList<>();

        try {
            IntStream.range(0, threadCount).forEach(i -> futures.add(executor.submit(() -> {
                startLatch.await();
                return action.get();
            })));

            startLatch.countDown();

            List<Submission> results = Collections.synchronizedList(new ArrayList<>());
            for (Future<Submission> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * A test double that behaves like Spring Session's per-request {@code HttpSessionWrapper}: a new Java object on every
     * call, but attribute reads/writes are delegated to a shared, session-id-keyed store rather than being local to this
     * instance - matching how a database-backed session repository actually works in production.
     */
    private static class SharedStoreMockHttpSession extends MockHttpSession {

        private final String forcedId;
        private final Map<String, Object> sharedAttributeStore;

        SharedStoreMockHttpSession(String forcedId, Map<String, Object> sharedAttributeStore) {
            this.forcedId = forcedId;
            this.sharedAttributeStore = sharedAttributeStore;
        }

        @Override
        public String getId() {
            return forcedId;
        }

        @Override
        public Object getAttribute(String name) {
            return sharedAttributeStore.get(name);
        }

        @Override
        public void setAttribute(String name, Object value) {
            sharedAttributeStore.put(name, value);
        }

        @Override
        public void removeAttribute(String name) {
            sharedAttributeStore.remove(name);
        }
    }
}
