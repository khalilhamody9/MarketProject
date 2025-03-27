package com.example.khalilo.models;

import com.google.gson.annotations.SerializedName;

public class GroupRequest {
    @SerializedName("id")
    private String id;

    @SerializedName("username")
    private String username;

    @SerializedName("groupNumber")
    private String groupNumber;

    @SerializedName("status")
    private String status;

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getGroupNumber() {
        return groupNumber;
    }

    public String getStatus() {
        return status;
    }
}
