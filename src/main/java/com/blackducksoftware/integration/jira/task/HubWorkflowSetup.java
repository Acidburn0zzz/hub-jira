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
package com.blackducksoftware.integration.jira.task;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.core.util.ClassLoaderUtils;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.workflow.AssignableWorkflowScheme;
import com.atlassian.jira.workflow.ConfigurableJiraWorkflow;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowUtil;
import com.blackducksoftware.integration.jira.common.HubJiraConstants;
import com.blackducksoftware.integration.jira.common.HubJiraLogger;
import com.blackducksoftware.integration.jira.task.issue.JiraServices;
import com.opensymphony.workflow.loader.WorkflowDescriptor;

public class HubWorkflowSetup {

	private final HubJiraLogger logger = new HubJiraLogger(Logger.getLogger(this.getClass().getName()));

	private final JiraSettingsService settingService;

	private final JiraServices jiraServices;

	public HubWorkflowSetup(final JiraSettingsService settingService, final JiraServices jiraServices) {
		this.settingService = settingService;
		this.jiraServices = jiraServices;
	}

	public JiraWorkflow addHubWorkflowToJira() {
		try {
			JiraWorkflow hubWorkflow = jiraServices.getWorkflowManager()
					.getWorkflow(HubJiraConstants.HUB_JIRA_WORKFLOW);
			if (hubWorkflow == null) {
				// https://developer.atlassian.com/confdev/development-resources/confluence-developer-faq/what-is-the-best-way-to-load-a-class-or-resource-from-a-plugin
				final InputStream inputStream = ClassLoaderUtils
						.getResourceAsStream(HubJiraConstants.HUB_JIRA_WORKFLOW_RESOURCE, this.getClass());
				if (inputStream == null) {
					logger.error("Could not find the Hub Jira workflow resource.");
					settingService.addHubError("Could not find the Hub Jira workflow resource.", "addHubWorkflow");
					return null;
				}
				final ApplicationUser jiraAppUser = getJiraSystemAdmin();
				if (jiraAppUser == null) {
					logger.error("Could not find any Jira System Admins to create the workflow.");
					return null;
				}
				final String workflowXml = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

				final WorkflowDescriptor workflowDescriptor = WorkflowUtil.convertXMLtoWorkflowDescriptor(workflowXml);

				hubWorkflow = new ConfigurableJiraWorkflow(HubJiraConstants.HUB_JIRA_WORKFLOW, workflowDescriptor,
						jiraServices.getWorkflowManager());

				jiraServices.getWorkflowManager().createWorkflow(jiraAppUser, hubWorkflow);
				logger.debug("Created the Hub Workflow : " + HubJiraConstants.HUB_JIRA_WORKFLOW);
			}
			return hubWorkflow;
		} catch (final Exception e) {
			logger.error("Failed to add the Hub Jira worflow.", e);
			settingService.addHubError(e, "addHubWorkflow");
		}
		return null;
	}

	public void addWorkflowToProjectsWorkflowScheme(final JiraWorkflow hubWorkflow, final Project project,
			final List<IssueType> issueTypes) {
		try {
			final AssignableWorkflowScheme projectWorkflowScheme = jiraServices.getWorkflowSchemeManager()
					.getWorkflowSchemeObj(project);

			if (projectWorkflowScheme != null) {
				final AssignableWorkflowScheme.Builder projectWorkflowSchemeBuilder = projectWorkflowScheme.builder();
				// FIXME should check if the workflow scheme is the default, we
				// dont
				// want to modify the default scheme right??

				boolean needsToBeUpdated = false;
				// IMPORTANT we assume our custom issue types are already in
				// this
				// Projects Workflow scheme
				if (issueTypes != null && !issueTypes.isEmpty()) {
					for (final IssueType issueType : issueTypes) {
						final String workflowName = projectWorkflowScheme.getConfiguredWorkflow(issueType.getId());

						if (StringUtils.isBlank(workflowName)) {
							projectWorkflowSchemeBuilder.setMapping(issueType.getId(), hubWorkflow.getName());
							logger.debug("Updating Jira Project : " + project.getName() + ", Issue Type : "
									+ issueType.getName() + ", to the Hub workflow '" + hubWorkflow.getName() + "'");
							needsToBeUpdated = true;
						} else {
							if (!workflowName.equals(hubWorkflow.getName())) {
								projectWorkflowSchemeBuilder.setMapping(issueType.getId(), hubWorkflow.getName());
								logger.debug("Updating Jira Project : " + project.getName() + ", Issue Type : "
										+ issueType.getName() + ", to the Hub workflow '" + hubWorkflow.getName()
										+ "'");
								needsToBeUpdated = true;
							}
						}
					}
				}
				if (needsToBeUpdated) {
					jiraServices.getWorkflowSchemeManager().updateWorkflowScheme(projectWorkflowSchemeBuilder.build());
				}
			} else {
				final String errorMessage = "Could not find the workflow scheme for the Jira project : "
						+ project.getName();
				logger.error(errorMessage);
				settingService.addHubError(errorMessage, null, null, project.getName(), null,
						"addWorkflowToProjectsWorkflowScheme");
			}
		} catch (final Exception e) {
			logger.error("Failed to add the Hub Jira worflow to the Hub scheme.", e);
			settingService.addHubError(e, null, null, project.getName(), null, "addWorkflowToProjectsWorkflowScheme");
		}
	}

	private ApplicationUser getJiraSystemAdmin() {
		final Collection<User> jiraSysAdmins = jiraServices.getUserUtil().getJiraSystemAdministrators();
		if (jiraSysAdmins == null || jiraSysAdmins.isEmpty()) {
			return null;
		}
		final User jiraSysAdmin = jiraSysAdmins.iterator().next();
		return jiraServices.userToApplicationUser(jiraSysAdmin);
	}

}
