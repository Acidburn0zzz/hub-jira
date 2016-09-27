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
package com.blackducksoftware.integration.jira.task.setup;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.jira.issue.issuetype.IssueType;
import com.blackducksoftware.integration.jira.task.JiraSettingsService;
import com.blackducksoftware.integration.jira.task.issue.JiraServices;

public class HubFieldScreenSchemeSetupJira6 extends AbstractHubFieldScreenSchemeSetup {

	public HubFieldScreenSchemeSetupJira6(final JiraSettingsService settingService, final JiraServices jiraServices) {
		super(settingService, jiraServices);
	}

	@Override
	protected List<Object> getIssueTypeObjectList(final List<IssueType> hubIssueTypes) {
		final List<Object> genericValues = new ArrayList<>();
		for (final IssueType hubIssueType : hubIssueTypes) {
			genericValues.add(hubIssueType.getGenericValue());
		}
		return genericValues;
	}

	@Override
	protected Object getIssueTypeObject(final IssueType hubIssueType) {
		return hubIssueType.getGenericValue();
	}


}