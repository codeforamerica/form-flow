package formflow.library.config;

import formflow.library.config.submission.Condition;
import formflow.library.data.Submission;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Looks up and runs the {@link Condition} beans configured for a flow's screens, by the condition's simple class
 * name.
 */
@Slf4j
@Component
public class ConditionManager {

    private final HashMap<String, Condition> conditions = new HashMap<>();

    /**
     * Indexes every {@link Condition} bean in the application context by its simple class name, so it can later
     * be looked up by name.
     *
     * @param conditionsList every {@link Condition} bean in the application context
     */
    public ConditionManager(List<Condition> conditionsList) {
        conditionsList.forEach(condition -> this.conditions.put(condition.getClass().getSimpleName(), condition));
    }

    /**
     * Looks up a {@link Condition} by its simple class name.
     *
     * @param name the simple class name of the condition to look up
     * @return the matching {@link Condition}, or null if none is registered under that name
     */
    public Condition getCondition(String name) {
        return conditions.get(name);
    }

    /**
     * Checks whether a {@link Condition} is registered under the given name.
     *
     * @param name the simple class name of the condition to check for
     * @return true if a condition is registered under that name, else false
     */
    public Boolean conditionExists(String name) {
        return conditions.containsKey(name);
    }

    /**
     * Runs a named condition against a submission. If no condition is registered under that name, logs a
     * warning and returns false rather than throwing.
     *
     * @param conditionName the simple class name of the condition to run
     * @param submission    submission object the condition is associated with, not null
     * @return true if the condition check passes, else false
     */
    public Boolean runCondition(String conditionName, Submission submission) {
        Condition condition = getCondition(conditionName);
        if (condition == null) {
            log.warn("Condition not found: " + conditionName);
            return false;
        }
        return condition.run(submission);
    }

    /**
     * Runs a named condition against a submission's subflow iteration. If no condition is registered under that
     * name, logs a warning and returns false rather than throwing.
     *
     * @param conditionName the simple class name of the condition to run
     * @param submission    submission object the condition is associated with, not null
     * @param uuid          uuid of the subflow iteration this should operate on
     * @return true if the condition check passes, else false
     */
    public Boolean runCondition(String conditionName, Submission submission, String uuid) {
        Condition condition = getCondition(conditionName);
        if (condition == null) {
            log.warn("Condition not found: " + conditionName);
            return false;
        }
        return condition.run(submission, uuid);
    }

    /**
     * Runs a named condition against a submission's subflow repeatFor iteration. If no condition is registered
     * under that name, logs a warning and returns false rather than throwing.
     *
     * @param conditionName the simple class name of the condition to run
     * @param submission    submission object the condition is associated with, not null
     * @param subflowUuid   uuid of the subflow iteration this should operate on
     * @param repeatForUuid uuid of the subflow's repeatFor iteration this should operate on
     * @return true if the condition check passes, else false
     */
    public Boolean runCondition(String conditionName, Submission submission, String subflowUuid, String repeatForUuid) {
        Condition condition = getCondition(conditionName);
        if (condition == null) {
            log.warn("Condition not found: " + conditionName);
            return false;
        }
        return condition.run(submission, subflowUuid, repeatForUuid);
    }
}
