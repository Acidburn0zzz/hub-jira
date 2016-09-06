package com.blackducksoftware.integration.jira.mocks;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.workflow.AssignableWorkflowScheme;

public class AssignableWorkflowSchemeMock implements AssignableWorkflowScheme {

	private final Map<String, String> mappingIssueTypeNamesToWorkFlowNames = new HashMap<>();

	private String name;

	private Builder builder;

	public void addMappingIssueToWorkflow(final String issueTypeName, final String workflowName) {
		mappingIssueTypeNamesToWorkFlowNames.put(issueTypeName, workflowName);
	}

	@Override
	public String getActualDefaultWorkflow() {

		return null;
	}

	@Override
	public String getActualWorkflow(final String arg0) {

		return null;
	}

	@Override
	public String getConfiguredDefaultWorkflow() {

		return null;
	}

	@Override
	public String getConfiguredWorkflow(final String issueId) {
		return mappingIssueTypeNamesToWorkFlowNames.get(issueId);
	}

	@Override
	public Long getId() {

		return null;
	}

	@Override
	public Map<String, String> getMappings() {

		return mappingIssueTypeNamesToWorkFlowNames;
	}

	@Override
	public boolean isDefault() {

		return false;
	}

	@Override
	public boolean isDraft() {

		return false;
	}

	@Override
	public Builder builder() {
		return builder;
	}

	public void setBuilder(final Builder builder) {
		this.builder = builder;
	}

	@Override
	public String getDescription() {

		return null;
	}

	@Override
	public String getName() {

		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

}