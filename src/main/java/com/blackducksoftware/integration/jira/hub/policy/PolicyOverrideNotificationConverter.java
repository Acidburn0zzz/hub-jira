package com.blackducksoftware.integration.jira.hub.policy;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.blackducksoftware.integration.hub.component.api.ComponentVersionStatus;
import com.blackducksoftware.integration.hub.exception.UnexpectedHubResponseException;
import com.blackducksoftware.integration.hub.notification.NotificationService;
import com.blackducksoftware.integration.hub.notification.NotificationServiceException;
import com.blackducksoftware.integration.hub.notification.api.NotificationItem;
import com.blackducksoftware.integration.hub.notification.api.PolicyOverrideNotificationItem;
import com.blackducksoftware.integration.hub.version.api.ReleaseItem;
import com.blackducksoftware.integration.jira.HubJiraLogger;
import com.blackducksoftware.integration.jira.config.HubProjectMappings;
import com.blackducksoftware.integration.jira.hub.HubEvents;
import com.blackducksoftware.integration.jira.hub.TicketGeneratorInfo;
import com.blackducksoftware.integration.jira.issue.HubEventType;

public class PolicyOverrideNotificationConverter extends PolicyNotificationConverter {
	private final HubJiraLogger logger = new HubJiraLogger(Logger.getLogger(this.getClass().getName()));

	public PolicyOverrideNotificationConverter(final HubProjectMappings mappings, final TicketGeneratorInfo ticketGenInfo,
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
			final PolicyOverrideNotificationItem ruleViolationNotif = (PolicyOverrideNotificationItem) notif;
			eventType = HubEventType.POLICY_OVERRIDE;

			compVerStatuses = new ArrayList<>();
			final ComponentVersionStatus componentStatus = new ComponentVersionStatus();
			componentStatus.setBomComponentVersionPolicyStatusLink(ruleViolationNotif.getContent()
					.getBomComponentVersionPolicyStatusLink());
			componentStatus.setComponentName(ruleViolationNotif.getContent().getComponentName());
			componentStatus.setComponentVersionLink(ruleViolationNotif.getContent().getComponentVersionLink());

			compVerStatuses.add(componentStatus);

			projectName = ruleViolationNotif.getContent().getProjectName();

			notifHubProjectReleaseItem = getHubNotificationService().getProjectReleaseItemFromProjectReleaseUrl(
					ruleViolationNotif.getContent().getProjectVersionLink());
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
