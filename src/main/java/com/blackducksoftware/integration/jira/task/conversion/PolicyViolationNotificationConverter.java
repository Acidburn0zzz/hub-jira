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
package com.blackducksoftware.integration.jira.task.conversion;

import java.util.ArrayList;
import java.util.List;

import com.blackducksoftware.integration.hub.api.policy.PolicyRule;
import com.blackducksoftware.integration.hub.dataservices.notification.items.NotificationContentItem;
import com.blackducksoftware.integration.hub.dataservices.notification.items.PolicyViolationContentItem;
import com.blackducksoftware.integration.hub.exception.NotificationServiceException;
import com.blackducksoftware.integration.hub.exception.UnexpectedHubResponseException;
import com.blackducksoftware.integration.jira.common.HubJiraConstants;
import com.blackducksoftware.integration.jira.common.HubProjectMappings;
import com.blackducksoftware.integration.jira.common.JiraContext;
import com.blackducksoftware.integration.jira.common.JiraProject;
import com.blackducksoftware.integration.jira.common.exception.ConfigurationException;
import com.blackducksoftware.integration.jira.config.HubJiraFieldCopyConfigSerializable;
import com.blackducksoftware.integration.jira.task.JiraSettingsService;
import com.blackducksoftware.integration.jira.task.conversion.output.HubEvent;
import com.blackducksoftware.integration.jira.task.conversion.output.HubEventAction;
import com.blackducksoftware.integration.jira.task.conversion.output.PolicyEvent;
import com.blackducksoftware.integration.jira.task.issue.JiraServices;

public class PolicyViolationNotificationConverter extends AbstractPolicyNotificationConverter {

    private final HubJiraFieldCopyConfigSerializable fieldCopyConfig;

    public PolicyViolationNotificationConverter(final HubProjectMappings mappings,
            final HubJiraFieldCopyConfigSerializable fieldCopyConfig,
            final JiraServices jiraServices,
            final JiraContext jiraContext, final JiraSettingsService jiraSettingsService)
            throws ConfigurationException {
        super(mappings, jiraServices, jiraContext, jiraSettingsService, HubJiraConstants.HUB_POLICY_VIOLATION_ISSUE);
        this.fieldCopyConfig = fieldCopyConfig;
    }

    @Override
    protected List<HubEvent> handleNotificationPerJiraProject(final NotificationContentItem notif,
            final JiraProject jiraProject) throws UnexpectedHubResponseException, NotificationServiceException {
        final List<HubEvent> events = new ArrayList<>();

        HubEventAction action = HubEventAction.OPEN;
        final PolicyViolationContentItem notification = (PolicyViolationContentItem) notif;
        for (final PolicyRule rule : notification.getPolicyRuleList()) {
            final HubEvent event = new PolicyEvent(action, getJiraContext().getJiraUser().getName(),
                    getJiraContext().getJiraUser().getKey(), jiraProject.getAssigneeUserId(),
                    getIssueTypeId(), jiraProject.getProjectId(), jiraProject.getProjectName(),
                    notification, rule,
                    null, HubJiraConstants.HUB_POLICY_VIOLATION_DETECTED_AGAIN_COMMENT,
                    HubJiraConstants.HUB_POLICY_VIOLATION_RESOLVE, fieldCopyConfig.getProjectFieldCopyMappings());
            events.add(event);
        }

        return events;
    }

}
