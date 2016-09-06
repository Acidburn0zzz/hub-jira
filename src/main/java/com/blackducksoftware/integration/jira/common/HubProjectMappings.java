/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.blackducksoftware.integration.jira.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.issue.issuetype.IssueType;
import com.blackducksoftware.integration.hub.exception.NotificationServiceException;
import com.blackducksoftware.integration.jira.task.issue.JiraServices;

public class HubProjectMappings {
	private final HubJiraLogger logger = new HubJiraLogger(Logger.getLogger(this.getClass().getName()));
	private final JiraContext jiraContext;
	private final Set<HubProjectMapping> mappings;
	private final JiraServices jiraServices;

	public HubProjectMappings(final JiraServices jiraServices, final JiraContext jiraContext,
			final Set<HubProjectMapping> mappings) {
		this.jiraServices = jiraServices;
		this.jiraContext = jiraContext;
		this.mappings = mappings;
	}

	public List<JiraProject> getJiraProjects(final String hubProjectName) {
		final List<JiraProject> matchingJiraProjects = new ArrayList<>();

		if (mappings == null || mappings.isEmpty()) {
			logger.debug("There are no configured project mapping");
			return matchingJiraProjects;
		}

		for (final HubProjectMapping mapping : mappings) {
			final JiraProject mappingJiraProject = mapping.getJiraProject();
			final JiraProject jiraProject;
			try {
				jiraProject = getJiraProject(mappingJiraProject.getProjectId());
			} catch (final NotificationServiceException e) {
				logger.warn("Mapped project '" + mappingJiraProject.getProjectName() + "' with ID "
						+ mappingJiraProject.getProjectId() + " not found in JIRA; skipping this notification");
				continue;
			}
			if (StringUtils.isNotBlank(jiraProject.getProjectError())) {
				logger.error(jiraProject.getProjectError());
				continue;
			}

			logger.debug("JIRA Project: " + jiraProject);

			final HubProject hubProject = mapping.getHubProject();

			// Check by name because the notifications may be for Hub projects
			// that the User doesnt have access to
			logger.debug("hubProject.getProjectName() (from config mapping): " + hubProject.getProjectName());
			logger.debug("hubProjectName (from notification content)       : " + hubProjectName);
			if ((!StringUtils.isBlank(hubProject.getProjectName())
					&& (hubProject.getProjectName().equals(hubProjectName)))) {
				logger.debug("Match!");
				matchingJiraProjects.add(jiraProject);
			}
		}
		logger.debug("Number of matches found: " + matchingJiraProjects.size());
		return matchingJiraProjects;
	}

	private JiraProject getJiraProject(final long jiraProjectId) throws NotificationServiceException {
		final com.atlassian.jira.project.Project atlassianJiraProject = jiraServices.getJiraProjectManager()
				.getProjectObj(jiraProjectId);
		if (atlassianJiraProject == null) {
			throw new NotificationServiceException("Error: JIRA Project with ID " + jiraProjectId + " not found");
		}
		final String jiraProjectKey = atlassianJiraProject.getKey();
		final String jiraProjectName = atlassianJiraProject.getName();
		final JiraProject bdsJiraProject = new JiraProject();
		bdsJiraProject.setProjectId(jiraProjectId);
		bdsJiraProject.setProjectKey(jiraProjectKey);
		bdsJiraProject.setProjectName(jiraProjectName);

		if (atlassianJiraProject.getIssueTypes() == null || atlassianJiraProject.getIssueTypes().isEmpty()) {
			bdsJiraProject.setProjectError("The Jira project : " + bdsJiraProject.getProjectName()
					+ " does not have any issue types, we will not be able to create tickets for this project.");
		} else {
			boolean projectHasIssueType = false;
			if (atlassianJiraProject.getIssueTypes() != null && !atlassianJiraProject.getIssueTypes().isEmpty()) {
				for (final IssueType issueType : atlassianJiraProject.getIssueTypes()) {
					if (issueType.getName().equals(jiraContext.getJiraIssueTypeName())) {
						bdsJiraProject.setIssueTypeId(issueType.getId());
						projectHasIssueType = true;
					}
				}
			}
			if (!projectHasIssueType) {
				bdsJiraProject.setProjectError(
						"The Jira project is missing the " + jiraContext.getJiraIssueTypeName() + " issue type.");
			}
		}
		return bdsJiraProject;
	}

	public int size() {
		if (mappings == null) {
			return 0;
		}
		return mappings.size();
	}
}