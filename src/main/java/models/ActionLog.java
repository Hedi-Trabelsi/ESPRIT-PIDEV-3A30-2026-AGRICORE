package models;

public class ActionLog {
    private int id;
    private String actionType; // CREATE, UPDATE, DELETE
    private String targetTable;
    private int targetId;
    private String description;
    private String createdAt;

    public ActionLog(String actionType, String targetTable, int targetId, String description) {
        this.actionType = actionType;
        this.targetTable = targetTable;
        this.targetId = targetId;
        this.description = description;
    }

    // Getters
    public int getId() { return id; }
    public String getActionType() { return actionType; }
    public String getTargetTable() { return targetTable; }
    public int getTargetId() { return targetId; }
    public String getDescription() { return description; }
    public String getCreatedAt() { return createdAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}