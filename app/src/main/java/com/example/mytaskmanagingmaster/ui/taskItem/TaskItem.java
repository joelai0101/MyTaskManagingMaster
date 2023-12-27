package com.example.mytaskmanagingmaster.ui.taskItem;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class TaskItem {
    private String name;
    private String desc;
    private LocalDate dueDate;
    private String id; // 新增任務 ID
    public TaskItem() {
        // Default constructor required for Firebase
    }

    public TaskItem(String id, String name, String desc, LocalDate dueDate) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.dueDate = dueDate;
    }

    public String getId() {
        return id != null ? id : ""; // 如果 id 為 null，則返回空字符串
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setId(String id) { this.id = id; }

    public void setName(String name) {
        this.name = name;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    @PropertyName("dueDate")
    public Map<String, String> serializeDueDate() {
        return LocalDateConverter.serialize(dueDate);
    }

    @PropertyName("dueDate")
    public void deserializeDueDate(Map<String, String> serialized) {
        dueDate = LocalDateConverter.deserialize(serialized);
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("id", id);
        taskMap.put("name", name);
        taskMap.put("desc", desc);
        taskMap.put("dueDate", serializeDueDate());

        return taskMap;
    }
}
