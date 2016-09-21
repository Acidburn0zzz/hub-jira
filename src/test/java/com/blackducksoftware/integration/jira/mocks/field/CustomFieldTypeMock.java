package com.blackducksoftware.integration.jira.mocks.field;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.plugin.customfield.CustomFieldTypeModuleDescriptor;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.web.bean.BulkEditBean;

public class CustomFieldTypeMock implements CustomFieldType {

	@Override
	public String availableForBulkEdit(final BulkEditBean arg0) {

		return null;
	}

	@Override
	public void createValue(final CustomField arg0, final Issue arg1, final Object arg2) {

	}

	@Override
	public String getChangelogString(final CustomField arg0, final Object arg1) {

		return null;
	}

	@Override
	public String getChangelogValue(final CustomField arg0, final Object arg1) {

		return null;
	}

	@Override
	public List getConfigurationItemTypes() {

		return null;
	}

	@Override
	public Object getDefaultValue(final FieldConfig arg0) {

		return null;
	}

	@Override
	public String getDescription() {

		return null;
	}

	@Override
	public CustomFieldTypeModuleDescriptor getDescriptor() {

		return null;
	}

	@Override
	public String getKey() {

		return null;
	}

	@Override
	public String getName() {

		return null;
	}

	@Override
	public List getRelatedIndexers(final CustomField arg0) {

		return null;
	}

	@Override
	public Object getSingularObjectFromString(final String arg0) throws FieldValidationException {

		return null;
	}

	@Override
	public String getStringFromSingularObject(final Object arg0) {

		return null;
	}

	@Override
	public Object getStringValueFromCustomFieldParams(final CustomFieldParams arg0) {

		return null;
	}

	@Override
	public Object getValueFromCustomFieldParams(final CustomFieldParams arg0) throws FieldValidationException {

		return null;
	}

	@Override
	public Object getValueFromIssue(final CustomField arg0, final Issue arg1) {

		return null;
	}

	@Override
	public Map getVelocityParameters(final Issue arg0, final CustomField arg1, final FieldLayoutItem arg2) {

		return null;
	}

	@Override
	public void init(final CustomFieldTypeModuleDescriptor arg0) {

	}

	@Override
	public boolean isRenderable() {

		return false;
	}

	@Override
	public Set remove(final CustomField arg0) {

		return null;
	}

	@Override
	public void setDefaultValue(final FieldConfig arg0, final Object arg1) {

	}

	@Override
	public void updateValue(final CustomField arg0, final Issue arg1, final Object arg2) {

	}

	@Override
	public void validateFromParams(final CustomFieldParams arg0, final ErrorCollection arg1, final FieldConfig arg2) {

	}

	@Override
	public boolean valuesEqual(final Object arg0, final Object arg1) {

		return false;
	}

}