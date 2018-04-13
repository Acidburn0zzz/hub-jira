/**
 * Hub JIRA Plugin
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
 */
package com.blackducksoftware.integration.jira.task;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.atlassian.jira.util.BuildUtilsInfoImpl;
import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.Credentials;
import com.blackducksoftware.integration.hub.HubSupportHelper;
import com.blackducksoftware.integration.hub.api.nonpublic.HubRegistrationRequestService;
import com.blackducksoftware.integration.hub.api.nonpublic.HubVersionRequestService;
import com.blackducksoftware.integration.hub.api.user.UserRequestService;
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.global.HubProxyInfo;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.model.view.UserView;
import com.blackducksoftware.integration.hub.proxy.ProxyInfo;
import com.blackducksoftware.integration.hub.rest.CredentialsRestConnection;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.service.HubServicesFactory;
import com.blackducksoftware.integration.jira.common.HubJiraLogger;
import com.blackducksoftware.integration.jira.common.HubProjectMapping;
import com.blackducksoftware.integration.jira.common.HubProjectMappings;
import com.blackducksoftware.integration.jira.common.JiraContext;
import com.blackducksoftware.integration.jira.common.PolicyRuleSerializable;
import com.blackducksoftware.integration.jira.common.TicketInfoFromSetup;
import com.blackducksoftware.integration.jira.config.HubJiraConfigSerializable;
import com.blackducksoftware.integration.jira.config.HubJiraFieldCopyConfigSerializable;
import com.blackducksoftware.integration.jira.task.issue.JiraServices;
import com.blackducksoftware.integration.phonehome.PhoneHomeClient;
import com.blackducksoftware.integration.phonehome.PhoneHomeRequestBody;
import com.blackducksoftware.integration.phonehome.enums.ProductIdEnum;
import com.blackducksoftware.integration.phonehome.google.analytics.GoogleAnalyticsConstants;
import com.blackducksoftware.integration.util.CIEnvironmentVariables;

public class HubJiraTask {
    private final HubJiraLogger logger = new HubJiraLogger(Logger.getLogger(this.getClass().getName()));

    private final PluginConfigurationDetails pluginConfigDetails;
    private final JiraContext jiraContext;
    private final Date runDate;
    private final String runDateString;
    private final SimpleDateFormat dateFormatter;
    private final JiraServices jiraServices = new JiraServices();
    private final JiraSettingsService jiraSettingsService;
    private final TicketInfoFromSetup ticketInfoFromSetup;
    private final String fieldCopyMappingJson;

    public HubJiraTask(final PluginConfigurationDetails configDetails, final JiraContext jiraContext, final JiraSettingsService jiraSettingsService, final TicketInfoFromSetup ticketInfoFromSetup) {
        this.pluginConfigDetails = configDetails;
        this.jiraContext = jiraContext;

        this.runDate = new Date();
        dateFormatter = new SimpleDateFormat(RestConnection.JSON_DATE_FORMAT);
        dateFormatter.setTimeZone(java.util.TimeZone.getTimeZone("Zulu"));
        this.runDateString = dateFormatter.format(runDate);
        logger.debug("Install date: " + configDetails.getInstallDateString());
        logger.debug("Last run date: " + configDetails.getLastRunDateString());

        this.jiraSettingsService = jiraSettingsService;
        this.ticketInfoFromSetup = ticketInfoFromSetup;
        this.fieldCopyMappingJson = configDetails.getFieldCopyMappingJson();

        logger.debug("createVulnerabilityIssues: " + configDetails.isCreateVulnerabilityIssues());
    }

    /**
     * Setup, then generate JIRA tickets based on recent notifications
     *
     * @return this execution's run date/time string on success, null otherwise
     */
    public String execute() {
        final HubServerConfigBuilder hubConfigBuilder = pluginConfigDetails.createHubServerConfigBuilder();
        HubServerConfig hubServerConfig = null;
        try {
            logger.debug("Building Hub configuration");
            hubServerConfig = hubConfigBuilder.build();
            logger.debug("Finished building Hub configuration");
        } catch (final IllegalStateException e) {
            logger.error(
                    "Unable to connect to the Hub. This could mean the Hub is currently unreachable, or that at least one of the Black Duck plugins (either the Hub Admin plugin or the Hub JIRA plugin) is not (yet) configured correctly: "
                            + e.getMessage());
            return "error";
        }

        final HubJiraConfigSerializable config = deSerializeConfig(hubServerConfig);
        if (config == null) {
            return null;
        }
        final HubJiraFieldCopyConfigSerializable fieldCopyConfig = deSerializeFieldCopyConfig();

        final Date startDate;
        try {
            startDate = deriveStartDate(pluginConfigDetails.getInstallDateString(), pluginConfigDetails.getLastRunDateString());
        } catch (final ParseException e) {
            logger.info(
                    "This is the first run, but the plugin install date cannot be parsed; Not doing anything this time, will record collection start time and start collecting notifications next time");
            return runDateString;
        }

        try {
            final HubServicesFactory hubServicesFactory;
            try {
                hubServicesFactory = createHubServicesFactory(hubServerConfig);
            } catch (final EncryptionException e) {
                logger.info("Error handling password: " + e.getMessage());
                return null;
            }
            final List<String> linksOfRulesToMonitor = getRuleUrls(config);
            final HubSupportHelper hubSupportHelper = new HubSupportHelper();
            final HubVersionRequestService hubVersionRequestService = hubServicesFactory.createHubVersionRequestService();
            hubSupportHelper.checkHubSupport(hubVersionRequestService, null);

            final TicketGenerator ticketGenerator = initTicketGenerator(jiraContext, hubServicesFactory,
                    linksOfRulesToMonitor, ticketInfoFromSetup, fieldCopyConfig, hubSupportHelper);

            // Phone-Home
            final LocalDate lastPhoneHome = jiraSettingsService.getLastPhoneHome();
            if (LocalDate.now().isAfter(lastPhoneHome)) {
                final HubVersionRequestService hubSupport = hubServicesFactory.createHubVersionRequestService();
                final HubRegistrationRequestService regService = hubServicesFactory.createHubRegistrationRequestService();
                try {
                    final String hubVersion = hubSupport.getHubVersion();
                    String regId = null;
                    String hubHostName = null;
                    try {
                        regId = regService.getRegistrationId();
                    } catch (final Exception e) {
                        regId = PhoneHomeRequestBody.Builder.UNKNOWN_ID;
                        logger.debug("Could not get the Hub registration Id.");
                    }
                    try {
                        hubHostName = hubServerConfig.getHubUrl().getHost();
                    } catch (final Exception e) {
                        hubHostName = PhoneHomeRequestBody.Builder.UNKNOWN_ID;
                        logger.debug("Could not get the Hub Host name.");
                    }
                    // TODO replace this conversion when hub-common is upgraded
                    final HubProxyInfo hubProxyInfo = hubServerConfig.getProxyInfo();
                    final ProxyInfo proxyInfo = new ProxyInfo(hubProxyInfo.getHost(), hubProxyInfo.getPort(), new Credentials(hubProxyInfo.getUsername(), hubProxyInfo.getDecryptedPassword(), false), hubProxyInfo.getIgnoredProxyHosts());
                    bdPhoneHome(hubVersion, regId, hubHostName, hubServerConfig.getTimeout(), proxyInfo, hubServerConfig.isAlwaysTrustServerCertificate());
                } catch (final Exception e) {
                    logger.debug("Unable to phone-home", e);
                }
            }
            final HubProjectMappings hubProjectMappings = new HubProjectMappings(jiraServices,
                    config.getHubProjectMappings());

            logger.debug("Getting user item for user: " + hubServerConfig.getGlobalCredentials().getUsername());
            final UserView hubUserItem = getHubUserItem(hubServicesFactory,
                    hubServerConfig.getGlobalCredentials().getUsername());
            if (hubUserItem == null) {
                return null;
            }
            // Generate JIRA Issues based on recent notifications
            logger.info("Getting Hub notifications from " + startDate + " to " + runDate);
            ticketGenerator.generateTicketsForRecentNotifications(hubUserItem, hubProjectMappings, startDate, runDate);
        } catch (final Exception e) {
            logger.error("Error processing Hub notifications or generating JIRA issues: " + e.getMessage(), e);
            jiraSettingsService.addHubError(e, "executeHubJiraTask");
            return null;
        }
        return runDateString;
    }

    private UserView getHubUserItem(final HubServicesFactory hubServicesFactory, final String currentUsername) {
        if (currentUsername == null) {
            final String msg = "Current username is null";
            logger.error(msg);
            jiraSettingsService.addHubError(msg, "getCurrentUser");
            return null;
        }
        final UserRequestService userService = hubServicesFactory.createUserRequestService();
        List<UserView> users;
        try {
            users = userService.getAllUsers();
        } catch (final IntegrationException e) {
            final String msg = "Error getting user item for current user: " + currentUsername + ": " + e.getMessage();
            logger.error(msg);
            jiraSettingsService.addHubError(msg, "getCurrentUser");
            return null;
        }
        for (final UserView user : users) {
            if (currentUsername.equalsIgnoreCase(user.userName)) {
                return user;
            }
        }
        final String msg = "Current user: " + currentUsername + " not found in list of all users";
        logger.error(msg);
        jiraSettingsService.addHubError(msg, "getCurrentUser");
        return null;
    }

    private HubServicesFactory createHubServicesFactory(final HubServerConfig hubServerConfig) throws EncryptionException {
        // TODO replace this conversion when hub-common is upgraded
        final HubProxyInfo hubProxyInfo = hubServerConfig.getProxyInfo();
        final ProxyInfo proxyInfo = new ProxyInfo(hubProxyInfo.getHost(), hubProxyInfo.getPort(), new Credentials(hubProxyInfo.getUsername(), hubProxyInfo.getDecryptedPassword(), false), hubProxyInfo.getIgnoredProxyHosts());
        final RestConnection restConnection = new CredentialsRestConnection(logger, hubServerConfig.getHubUrl(), hubServerConfig.getGlobalCredentials().getUsername(), hubServerConfig.getGlobalCredentials().getDecryptedPassword(),
                hubServerConfig.getTimeout(), proxyInfo);
        final HubServicesFactory hubServicesFactory = new HubServicesFactory(restConnection);
        return hubServicesFactory;
    }

    private List<String> getRuleUrls(final HubJiraConfigSerializable config) {
        final List<String> ruleUrls = new ArrayList<>();
        final List<PolicyRuleSerializable> rules = config.getPolicyRules();
        for (final PolicyRuleSerializable rule : rules) {
            final String ruleUrl = rule.getPolicyUrl();
            logger.debug("getRuleUrls(): rule name: " + rule.getName() + "; ruleUrl: " + ruleUrl + "; checked: "
                    + rule.getChecked());
            if ((rule.getChecked()) && (!ruleUrl.equals("undefined"))) {
                ruleUrls.add(ruleUrl);
            }
        }
        return ruleUrls;
    }

    private TicketGenerator initTicketGenerator(final JiraContext jiraContext, final HubServicesFactory hubServicesFactory, final List<String> linksOfRulesToMonitor, final TicketInfoFromSetup ticketInfoFromSetup,
            final HubJiraFieldCopyConfigSerializable fieldCopyConfig, final HubSupportHelper hubSupportHelper) throws URISyntaxException {
        logger.debug("JIRA user: " + this.jiraContext.getJiraAdminUser().getName());

        final TicketGenerator ticketGenerator = new TicketGenerator(hubServicesFactory, jiraServices, jiraContext, jiraSettingsService, ticketInfoFromSetup, fieldCopyConfig, pluginConfigDetails.isCreateVulnerabilityIssues(),
                linksOfRulesToMonitor, hubSupportHelper);
        return ticketGenerator;
    }

    private HubJiraConfigSerializable deSerializeConfig(final HubServerConfig hubServerConfig) {
        if (pluginConfigDetails.getProjectMappingJson() == null) {
            logger.debug("HubNotificationCheckTask: Project Mappings not configured, therefore there is nothing to do.");
            return null;
        }

        if (pluginConfigDetails.getPolicyRulesJson() == null) {
            logger.debug("HubNotificationCheckTask: Policy Rules not configured, therefore there is nothing to do.");
            return null;
        }

        logger.debug("Last run date: " + pluginConfigDetails.getLastRunDateString());
        logger.debug("Hub url / username: " + hubServerConfig.getHubUrl().toString() + " / " + hubServerConfig.getGlobalCredentials().getUsername());
        logger.debug("Interval: " + pluginConfigDetails.getIntervalString());

        final HubJiraConfigSerializable config = new HubJiraConfigSerializable();
        config.setHubProjectMappingsJson(pluginConfigDetails.getProjectMappingJson());
        config.setPolicyRulesJson(pluginConfigDetails.getPolicyRulesJson());
        logger.debug("Mappings:");
        for (final HubProjectMapping mapping : config.getHubProjectMappings()) {
            logger.debug(mapping.toString());
        }
        logger.debug("Policy Rules:");
        for (final PolicyRuleSerializable rule : config.getPolicyRules()) {
            logger.debug(rule.toString());
        }
        return config;
    }

    private HubJiraFieldCopyConfigSerializable deSerializeFieldCopyConfig() {
        final HubJiraFieldCopyConfigSerializable fieldCopyConfig = new HubJiraFieldCopyConfigSerializable();
        fieldCopyConfig.setJson(fieldCopyMappingJson);
        return fieldCopyConfig;
    }

    private Date deriveStartDate(final String installDateString, final String lastRunDateString) throws ParseException {
        final Date startDate;
        if (lastRunDateString == null) {
            logger.info("No lastRunDate set, so this is the first run; Will collect notifications since the plugin install time: " + installDateString);
            startDate = dateFormatter.parse(installDateString);
        } else {
            startDate = dateFormatter.parse(lastRunDateString);
        }
        return startDate;
    }

    public void bdPhoneHome(final String blackDuckVersion, final String regId, final String hubHostName, final int timeout, final ProxyInfo proxyInfo, final boolean alwaysTrustServerCertificate) throws IntegrationException {
        final String jiraVersion = new BuildUtilsInfoImpl().getVersion();
        final String pluginVersion = jiraServices.getPluginVersion();

        final PhoneHomeRequestBody.Builder builder = new PhoneHomeRequestBody.Builder();
        builder.setCustomerId(regId);
        builder.setHostName(hubHostName);

        builder.setArtifactId("hub-jira");
        builder.setArtifactVersion(pluginVersion);

        builder.setProductId(ProductIdEnum.HUB);
        builder.setProductVersion(blackDuckVersion);

        builder.addToMetaData("jira.version", jiraVersion);

        try {
            final PhoneHomeClient phClient = new PhoneHomeClient(logger, GoogleAnalyticsConstants.PRODUCTION_INTEGRATIONS_TRACKING_ID, timeout, proxyInfo, alwaysTrustServerCertificate);
            final PhoneHomeRequestBody body = builder.build();
            phClient.postPhoneHomeRequest(body, new CIEnvironmentVariables());
            jiraSettingsService.setLastPhoneHome(LocalDate.now());
        } catch (final IllegalStateException e) {
            throw new IntegrationException(e);
        }
    }
}
