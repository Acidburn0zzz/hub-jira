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
var statusMessageFieldId = "aui-hub-message-field";
var statusMessageTitleId = "aui-hub-message-title";
var statusMessageTitleTextId = "aui-hub-message-title-text";
var statusMessageTextId = "aui-hub-message-text";

var errorStatus = "error";
var successStatus = "success";

var spinning = false;

function updateConfig() {
		putConfig(AJS.contextPath() + '/rest/hub-jira-integration/1.0/', 'Save successful.', 'The configuration is not valid.');
	}

function putConfig(restUrl, successMessage, failureMessage) {
	  AJS.$.ajax({
		    url: restUrl,
		    type: "PUT",
		    dataType: "json",
		    contentType: "application/json",
		    data: '{ "checkHowOften": "' + encodeURI(AJS.$("#checkHowOften").val())
		    + '"}',
		    processData: false,
		    success: function() {
			    showStatusMessage(successStatus, 'Success!', successMessage);
			    stopProgressSpinner();
		    },
		    error: function(response){
		    	var config = JSON.parse(response.responseText);
		    	handleError('checkHowOftenError', config.checkHowOften);
			    
			    showStatusMessage(errorStatus, 'ERROR!', failureMessage);
			    stopProgressSpinner();
		    }
		  });
}

function populateForm() {
	  AJS.$.ajax({
	    url: AJS.contextPath() + "/rest/hub-jira-integration/1.0/",
	    dataType: "json",
	    success: function(config) {
	      updateValue("checkHowOften", config.checkHowOften);
	      
	      handleError('checkHowOftenError', config.checkHowOftenError);
	    }
	  });
	}

function updateValue(fieldId, configField) {
	if(configField){
		 AJS.$("#" + fieldId).val(decodeURI(configField));
    }
}

function handleError(fieldId, configField) {
	if(configField){
		showError(fieldId, configField);
    } else{
    	hideError(fieldId);
    }
}

function showError(fieldId, configField) {
	  AJS.$("#" + fieldId).text(decodeURI(configField));
  	  removeClassFromField(fieldId, 'hidden');
}

function hideError(fieldId) {
	  AJS.$("#" + fieldId).text('');
  	  addClassToField(fieldId, 'hidden');
}

function showStatusMessage(status, statusTitle, message) {
	resetStatusMessage();
	if(status == errorStatus){
		addClassToField(statusMessageFieldId, 'error');
		addClassToField(statusMessageTitleId, 'icon-error');
	} else if (status == successStatus){
		addClassToField(statusMessageFieldId, 'success');
		addClassToField(statusMessageTitleId, 'icon-success');
	}
	AJS.$("#" + statusMessageTitleTextId).text(statusTitle);
	AJS.$("#" + statusMessageTextId).text(message);
	removeClassFromField(statusMessageFieldId, 'hidden');
}

function resetStatusMessage() {
	removeClassFromField(statusMessageFieldId,'error');
	removeClassFromField(statusMessageFieldId,'success');
	removeClassFromField(statusMessageTitleId,'icon-error');
	removeClassFromField(statusMessageTitleId,'icon-success');
	AJS.$("#" + statusMessageTitleTextId).text('');
	AJS.$("#" + statusMessageTextId).text('');
	addClassToField(statusMessageFieldId, 'hidden');
}

function addClassToField(fieldId, cssClass){
	if(!AJS.$("#" + fieldId).hasClass(cssClass)){
		AJS.$("#" + fieldId).addClass(cssClass);
	}
}

function removeClassFromField(fieldId, cssClass){
	if(AJS.$("#" + fieldId).hasClass(cssClass)){
		AJS.$("#" + fieldId).removeClass(cssClass);
	}
}

function startProgressSpinner(){
	 if (!spinning) {
		 var spinner = AJS.$('.spinner');
		 AJS.$('.spinner').spin('large');
		 spinning = true;
	 }
}

function stopProgressSpinner(){
	 if (spinning) {
		 AJS.$('.spinner').spinStop();
         spinning = false;
	 }
}

(function ($) {
	populateForm();
})(AJS.$ || jQuery);

