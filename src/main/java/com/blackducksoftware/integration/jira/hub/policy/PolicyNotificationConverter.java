package com.blackducksoftware.integration.jira.hub.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.blackducksoftware.integration.hub.component.api.BomComponentVersionPolicyStatus;
import com.blackducksoftware.integration.hub.component.api.ComponentVersionStatus;
import com.blackducksoftware.integration.hub.exception.MissingUUIDException;
import com.blackducksoftware.integration.hub.exception.UnexpectedHubResponseException;
import com.blackducksoftware.integration.hub.notification.NotificationService;
import com.blackducksoftware.integration.hub.notification.NotificationServiceException;
import com.blackducksoftware.integration.hub.policy.api.PolicyRule;
import com.blackducksoftware.integration.hub.version.api.ReleaseItem;
import com.blackducksoftware.integration.jira.HubJiraLogger;
import com.blackducksoftware.integration.jira.config.HubProjectMappings;
import com.blackducksoftware.integration.jira.config.JiraProject;
import com.blackducksoftware.integration.jira.hub.HubEvent;
import com.blackducksoftware.integration.jira.hub.HubEventAction;
import com.blackducksoftware.integration.jira.hub.JiraContext;
import com.blackducksoftware.integration.jira.hub.NotificationToEventConverter;
import com.blackducksoftware.integration.jira.hub.PolicyEvent;
import com.blackducksoftware.integration.jira.issue.HubEventType;
import com.blackducksoftware.integration.jira.issue.JiraServices;

public abstract class PolicyNotificationConverter extends NotificationToEventConverter {
	private final HubJiraLogger logger = new HubJiraLogger(Logger.getLogger(this.getClass().getName()));
	public static final String PROJECT_LINK = "project";
	private final HubProjectMappings mappings;
	private final List<String> linksOfRulesToMonitor;

	protected boolean isRulesToMonitor() {
		if (linksOfRulesToMonitor == null) {
			return false;
		}
		return (linksOfRulesToMonitor.size() > 0);
	}

	public PolicyNotificationConverter(final HubProjectMappings mappings, final JiraServices jiraServices,
			final JiraContext jiraContext, final List<String> linksOfRulesToMonitor,
			final NotificationService hubNotificationService) {
		super(hubNotificationService, jiraServices, jiraContext);
		this.mappings = mappings;
		this.linksOfRulesToMonitor = linksOfRulesToMonitor;
	}

	protected List<HubEvent> handleNotification(final HubEventType eventType,
			final String projectName, final String projectVersionName,
			final List<ComponentVersionStatus> compVerStatuses, final ReleaseItem notifHubProjectReleaseItem)
					throws UnexpectedHubResponseException, NotificationServiceException {
		final List<HubEvent> notifResults = new ArrayList<>();

		final String projectUrl = getProjectLink(notifHubProjectReleaseItem);

		for (final JiraProject mappingJiraProject : mappings.getJiraProjects(projectUrl)) {
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

			final List<HubEvent> oneProjectsResults = handleNotificationPerJiraProject(eventType,
					projectName, projectVersionName, compVerStatuses, notifHubProjectReleaseItem, jiraProject);
			if (oneProjectsResults != null) {
				notifResults.addAll(oneProjectsResults);
			}
		}
		return notifResults;
	}

	private List<HubEvent> handleNotificationPerJiraProject(final HubEventType eventType,
			final String projectName, final String projectVersionName,
			final List<ComponentVersionStatus> compVerStatuses, final ReleaseItem notifHubProjectReleaseItem,
			final JiraProject jiraProject)
					throws UnexpectedHubResponseException, NotificationServiceException {
		final List<HubEvent> events = new ArrayList<>();
		if ((linksOfRulesToMonitor == null) || (linksOfRulesToMonitor.size() == 0)) {
			logger.warn("No rules-to-monitor provided, skipping policy notifications.");
			return null;
		}
		for (final ComponentVersionStatus compVerStatus : compVerStatuses) {
			if (eventType == HubEventType.POLICY_VIOLATION
					&& compVerStatus.getComponentVersionLink() == null) {
				// FIXME see HUB-7571
				logger.error(
						"Cannot create tickets for component level violations at this time. This will be fixed in future releases.");
				continue;
			}
			final String componentVersionName = getHubNotificationService()
					.getComponentVersion(
							compVerStatus.getComponentVersionLink()).getVersionName();

			final String policyStatusUrl = compVerStatus.getBomComponentVersionPolicyStatusLink();

			final BomComponentVersionPolicyStatus bomComponentVersionPolicyStatus = getHubNotificationService()
					.getPolicyStatus(policyStatusUrl);

			logger.debug("BomComponentVersionPolicyStatus: " + bomComponentVersionPolicyStatus);
			final List<String> monitoredUrls = getMonitoredRules(bomComponentVersionPolicyStatus
					.getLinks(BomComponentVersionPolicyStatus.POLICY_RULE_URL));
			if(monitoredUrls == null || monitoredUrls.isEmpty()){
				logger.warn(
						"No configured policy violations matching this notification found; skipping this notification");
				continue;
			}

			for (final String ruleUrl : monitoredUrls) {
				final PolicyRule rule = getHubNotificationService().getPolicyRule(ruleUrl);
				logger.debug("Rule : " + rule);

				if (rule.getExpression() != null && rule.getExpression().hasOnlyProjectLevelConditions()) {
					logger.warn("Skipping this Violation since it is a Project only violation.");
					continue;
				}

				UUID versionId;
				UUID componentId;
				UUID componentVersionId;
				UUID ruleId;
				try {
					versionId = notifHubProjectReleaseItem.getVersionId();

					componentId = compVerStatus.getComponentId();

					componentVersionId = compVerStatus.getComponentVersionId();

					ruleId = rule.getPolicyRuleId();
				} catch (final MissingUUIDException e) {
					logger.error(e);
					continue;
				}

				HubEventAction action;
				if (eventType == HubEventType.POLICY_VIOLATION) {
					action = HubEventAction.OPEN;
				} else {
					action = HubEventAction.CLOSE;
				}
				final HubEvent event = new PolicyEvent(action, projectName,
						projectVersionName, compVerStatus.getComponentName(), componentVersionName, versionId,
						componentId, componentVersionId,
						getJiraContext().getJiraUser().getName(),
						jiraProject.getIssueTypeId(),
						jiraProject.getProjectId(), jiraProject.getProjectName(),
						eventType, rule, ruleId);

				if (event.getEventType() == HubEventType.POLICY_VIOLATION) {
					events.add(event);
				} else if (event.getEventType() == HubEventType.POLICY_OVERRIDE) {
					events.add(event);
				}
			}
		}
		return events;
	}

	private List<String> getMonitoredRules(final List<String> rulesViolated) throws NotificationServiceException {
		logger.debug("getMonitoredRules(): Configured rules to monitor: " + linksOfRulesToMonitor);
		if (rulesViolated == null || rulesViolated.isEmpty()) {
			logger.warn("No violated Rules provided.");
			return null;
		}
		final List<String> matchingRules = new ArrayList<>();
		for (final String ruleViolated : rulesViolated) {
			logger.debug("Violated rule (original): " + ruleViolated);
			final String fixedRuleUrl = fixRuleUrl(ruleViolated);
			logger.debug("Checking configured rules to monitor for fixed url: " + fixedRuleUrl);
			if (linksOfRulesToMonitor.contains(fixedRuleUrl)) {
				logger.debug("Monitored Rule : " + fixedRuleUrl);
				matchingRules.add(fixedRuleUrl);
			}
		}
		return matchingRules;
	}

	/**
	 * In Hub versions prior to 3.2, the rule URLs contained in notifications
	 * are internal. To match the configured rule URLs, the "internal" segment
	 * of the URL from the notification must be removed. This is the workaround
	 * recommended by Rob P. In Hub 3.2 on, these URLs will exclude the
	 * "internal" segment.
	 *
	 * @param origRuleUrl
	 * @return
	 */
	private String fixRuleUrl(final String origRuleUrl) {
		String fixedRuleUrl = origRuleUrl;
		if (origRuleUrl.contains("/internal/")) {
			fixedRuleUrl = origRuleUrl.replace("/internal/", "/");
			logger.debug("Adjusted rule URL from " + origRuleUrl + " to " + fixedRuleUrl);
		}
		return fixedRuleUrl;
	}

	private String getProjectLink(final ReleaseItem version) throws UnexpectedHubResponseException {
		final List<String> projectLinks = version.getLinks(PROJECT_LINK);
		if (projectLinks.size() != 1) {
			throw new UnexpectedHubResponseException("The release " + version.getVersionName() + " has "
					+ projectLinks.size() + " " + PROJECT_LINK + " links; expected one");
		}
		final String projectLink = projectLinks.get(0);
		return projectLink;
	}
}