package com.crm.entity;

public enum LeadStage {
    INTERESTED("Interested"),
    NOT_INTERESTED("Not Interested"),
    NORMAL("Normal");

    private final String displayName;

    LeadStage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}