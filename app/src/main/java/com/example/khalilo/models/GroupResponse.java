package com.example.khalilo.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GroupResponse {
    @SerializedName("message")
    private String message;

    @SerializedName("isAdmin")
    private boolean isAdmin;

    @SerializedName("requests")
    private List<GroupRequest> requests;

    @SerializedName("groups")
    private List<String> groups;  // Add this line

    // Getter for message
    public String getMessage() {
        return message;
    }

    // Getter for isAdmin
    public boolean isAdmin() {
        return isAdmin;
    }

    // Getter for requests
    public List<GroupRequest> getRequests() {
        return requests;
    }

    // Getter for groups
    public List<String> getGroups() {
        return groups;
    }

    // Setter for groups (optional, if needed)
    public void setGroups(List<String> groups) {
        this.groups = groups;
    }
}
