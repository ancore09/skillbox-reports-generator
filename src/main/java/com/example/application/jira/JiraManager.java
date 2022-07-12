package com.example.application.jira;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

import java.net.URI;

public class JiraManager {

    private static JiraRestClient client;
    private static boolean isConnected = false;
    public static boolean getConnectionState() {
        return isConnected;
    }

    public static void connectClient() {
        client = new AsynchronousJiraRestClientFactory()
                .createWithBasicHttpAuthentication(
                        URI.create("http://localhost:8081"), "admin", "admin");
        isConnected = true;
    }

    public static String createIssue(String projectKey, Long issueType, String issueSummary, String description) {
        IssueRestClient issueClient = client.getIssueClient();
        IssueInputBuilder builder = new IssueInputBuilder(projectKey, issueType, issueSummary);
        builder.setDescription(description);
        IssueInput newIssue = builder.build();
        return issueClient.createIssue(newIssue).claim().getKey();
    }
}
