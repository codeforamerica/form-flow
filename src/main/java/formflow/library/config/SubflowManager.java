package formflow.library.config;

import formflow.library.data.Submission;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SubflowManager {

    List<FlowConfiguration> flowConfigurations;
    
    public SubflowManager(List<FlowConfiguration> flowConfigurations) {
        this.flowConfigurations = flowConfigurations;
    }

    public boolean subflowHasRelationship(String flow, String subflow) {
        FlowConfiguration flowConfiguration = getFlowConfiguration(flow);
        if (flowConfiguration.getSubflows().containsKey(subflow)) {
            return flowConfiguration.getSubflows().get(subflow).getRelationship() != null;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Subflow %s not found in flow %s. Check that your flows-config.yaml is configured correctly.", subflow, flow));
        }
    }
    
    public void addSubflowRelationshipData(String flow, String subflow, String iterationId, Submission submission) {
        if (!subflowHasRelationship(flow, subflow)) {
            return;
        }
        
        SubflowConfiguration currentSubflow = getSubflowConfiguration(flow, subflow);
        String relatedSubflowName = currentSubflow.getRelationship().getRelatesTo();
        List<Map<String, Object>> currentSubflowData = (List<Map<String, Object>>) submission.getInputData().get(subflow);
        List<Map<String, Object>> relatedSubflowData = (List<Map<String, Object>>) submission.getInputData().get(relatedSubflowName);
        
        if (currentSubflowData.size() >= relatedSubflowData.size()) {
            return;
        }
        
        Set<String> usedRelationIds = new HashSet<>();
        for (Map<String, Object> subflowIteration : currentSubflowData) {
            String relationIdKey = getRelationAlias(flow, subflow).orElse("relationId");
            String relatedSubflowIterationId = (String) subflowIteration.get(relationIdKey);
            if (relatedSubflowIterationId != null) {
                usedRelationIds.add(relatedSubflowIterationId);
            }
        } 
        
        for (Map<String, Object> subflowIteration : relatedSubflowData) {
            String relatedSubflowIterationId = subflowIteration.get("uuid").toString();
            if (relatedSubflowIterationId != null && !usedRelationIds.contains(relatedSubflowIterationId)) {
                Map<String, Object> currentSubflowIteration = submission.getSubflowEntryByUuid(subflow, iterationId);
                currentSubflowIteration.put(getRelationAlias(flow, subflow).orElse("relationId"), relatedSubflowIterationId);
                usedRelationIds.add(relatedSubflowIterationId);
                break; // we only want to add one relationship each time this is called (e.g. for each iteration of the related subflow)
            }
        }
    }

    public boolean hasFinishedIteratingRelatedSubflow(String currentSubflowName, Submission submission) {
        String relatedSubflowName = getRelatedSubflowName(submission.getFlow(), currentSubflowName);
        List<Map<String, Object>> currentSubflowData = (List<Map<String, Object>>) submission.getInputData().get(currentSubflowName);
        List<Map<String, Object>> relatedSubflowData = (List<Map<String, Object>>) submission.getInputData().get(relatedSubflowName);

        return currentSubflowData.size() >= relatedSubflowData.size();
    }
    
    public String getIterationStartScreenForSubflow(String flowName, String subflowName) {
        FlowConfiguration flowConfiguration = getFlowConfiguration(flowName);
        if (flowConfiguration.getSubflows().get(subflowName) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Subflow %s not found in flow %s. Check that your flows-config.yaml is configured correctly.", subflowName, flowName));
        }
        return flowConfiguration.getSubflows().get(subflowName).getIterationStartScreen();
    }

    private FlowConfiguration getFlowConfiguration(String flowName) {
        FlowConfiguration flowConfiguration = flowConfigurations.stream()
                .filter(config -> config.getName().equals(flowName))
                .findFirst()
                .orElse(null);

        if (flowConfiguration == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Flow %s not found in your flows-config yaml file.", flowName));
        }
        return flowConfiguration;
    }

    public String getRelatedSubflowName(String flowName, String currentSubflowName) {
        FlowConfiguration flowConfiguration = getFlowConfiguration(flowName);
        
        if (flowConfiguration.getSubflows().containsKey(currentSubflowName)) {
            return flowConfiguration.getSubflows().get(currentSubflowName).getRelationship().getRelatesTo();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Subflow %s not found in flow %s. Is it possible you entered an incorrect subflow name in your subflow configuration?", currentSubflowName, flowName));
        }
    }
    
    public Map<String, Object> getRelatedSubflowIteration(String flowName, String subflowName, String currentScreen, Submission submission) {
        List<Map<String, Object>> subflowData = (List<Map<String, Object>>) submission.getInputData().get(subflowName);
        String relatedSubflowName = getRelatedSubflowName(flowName, subflowName);
        List<Map<String, Object>> relatedSubflowData = (List<Map<String, Object>>) submission.getInputData().get(relatedSubflowName);


        if (subflowData == null) {
            // We must be in a GET to /new for the first time in this subflow so the subflow isn't in the submission yet
            // return the first item in the related subflow since the first iteration should be for the first item
            return relatedSubflowData.getFirst();
        }

        int currentSubflowIteration = subflowData.size();
        // Return the index of the current subflow in the related subflow (e.g. if we are in the second iteration of the current subflow
        // We know the related subflow iteration is the item at index 1 of the related subflow considering 0 indexing)
        
        boolean isIterationStartScreen = getIterationStartScreenForSubflow(flowName, subflowName).equals(currentScreen);
        // We don't update the current subflows array size until the POST if we are on an iteration start screen so we need to
        // Add 1 to the index we call on the related subflow if we are on the iteration start screen (adding 1 to the index is the
        // same as just calling size() since the index is 0 based)
        return isIterationStartScreen ? relatedSubflowData.get(currentSubflowIteration) : relatedSubflowData.get(currentSubflowIteration - 1);
    }
    
    private Optional<String> getRelationAlias(String flow, String subflow) {
        return flowConfigurations.stream()
                .filter(config -> config.getName().equals(flow))
                .findFirst()
                .map(config -> config.getSubflows().get(subflow))
                .map(SubflowConfiguration::getRelationship)
                .map(SubflowRelationship::getRelationAlias);
    }

    private SubflowConfiguration getSubflowConfiguration(String flow, String subflow) {
        return flowConfigurations.stream()
                .filter(config -> config.getName().equals(flow))
                .findFirst().get().getSubflows().get(subflow);
    }
}
