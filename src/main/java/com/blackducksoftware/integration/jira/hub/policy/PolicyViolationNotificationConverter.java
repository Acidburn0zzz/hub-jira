package com.blackducksoftware.integration.jira.hub.policy;

import java.util.List;

import org.apache.log4j.Logger;

import com.blackducksoftware.integration.hub.component.api.ComponentVersionStatus;
import com.blackducksoftware.integration.hub.exception.UnexpectedHubResponseException;
import com.blackducksoftware.integration.hub.notification.NotificationService;
import com.blackducksoftware.integration.hub.notification.NotificationServiceException;
import com.blackducksoftware.integration.hub.notification.api.NotificationItem;
import com.blackducksoftware.integration.hub.notification.api.RuleViolationNotificationItem;
import com.blackducksoftware.integration.hub.version.api.ReleaseItem;
import com.blackducksoftware.integration.jira.HubJiraLogger;
import com.blackducksoftware.integration.jira.config.HubProjectMappings;
import com.blackducksoftware.integration.jira.hub.HubEvents;
import com.blackducksoftware.integration.jira.hub.TicketGeneratorInfo;
import com.blackducksoftware.integration.jira.issue.HubEventType;

public class PolicyViolationNotificationConverter extends PolicyNotificationConverter {
	private final HubJiraLogger logger = new HubJiraLogger(Logger.getLogger(this.getClass().getName()));

	public PolicyViolationNotificationConverter(final HubProjectMappings mappings,
			final TicketGeneratorInfo ticketGenInfo,
			final List<String> linksOfRulesToMonitor, final NotificationService hubNotificationService) {
		super(mappings, ticketGenInfo, linksOfRulesToMonitor, hubNotificationService);
	}

	@Override
	public HubEvents generateEvents(final NotificationItem notif) {
		HubEvents events;

		HubEventType eventType;
		String projectName;
		String projectVersionName;
		List<ComponentVersionStatus> compVerStatuses;
		final ReleaseItem notifHubProjectReleaseItem;

		try {
			final RuleViolationNotificationItem ruleViolationNotif = (RuleViolationNotificationItem) notif;
			eventType = HubEventType.POLICY_VIOLATION;
			compVerStatuses = ruleViolationNotif.getContent().getComponentVersionStatuses();
			projectName = ruleViolationNotif.getContent().getProjectName();
			notifHubProjectReleaseItem = getHubNotificationService()
					.getProjectReleaseItemFromProjectReleaseUrl(ruleViolationNotif.getContent().getProjectVersionLink());
			projectVersionName = notifHubProjectReleaseItem.getVersionName();
		} catch (final NotificationServiceException | UnexpectedHubResponseException e) {
			logger.error(e);
			return null;
		}

		try {
			events = handleNotification(eventType, projectName, projectVersionName, compVerStatuses,
					notifHubProjectReleaseItem);
		} catch (UnexpectedHubResponseException | NotificationServiceException e) {
			logger.error(e);
			return null;
		}
		return events;
	}
}
