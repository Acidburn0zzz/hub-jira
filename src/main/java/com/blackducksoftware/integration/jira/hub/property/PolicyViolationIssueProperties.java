package com.blackducksoftware.integration.jira.hub.property;

public class PolicyViolationIssueProperties {
	private final String projectName;
	private final String projectVersion;
	private final String componentName;
	private final String componentVersion;
	private final String ruleName;

	private final Long jiraIssueId;

	public PolicyViolationIssueProperties(final String projectName, final String projectVersion,
			final String componentName, final String componentVersion, final String ruleName,
			final Long jiraIssueId) {
		this.projectName = projectName;
		this.projectVersion = projectVersion;
		this.componentName = componentName;
		this.componentVersion = componentVersion;
		this.ruleName = ruleName;
		this.jiraIssueId = jiraIssueId;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getProjectVersion() {
		return projectVersion;
	}

	public String getComponentName() {
		return componentName;
	}

	public String getComponentVersion() {
		return componentVersion;
	}

	public String getRuleName() {
		return ruleName;
	}

	public Long getJiraIssueId() {
		return jiraIssueId;
	}

}