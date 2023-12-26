package com.example.mytaskmanagingmaster.ui.home;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<List<TaskItem>> taskItems;
    private DatabaseReference databaseReference;

    public HomeViewModel() {
        taskItems = new MutableLiveData<>();
        taskItems.setValue(new ArrayList<>());

        // 初始化 Firebase Database 引用
        databaseReference = FirebaseDatabase.getInstance().getReference("tasks");

        // 設定 Firebase Database 的監聽器
        setupFirebaseListener();
    }

    public LiveData<List<TaskItem>> getTaskItems() {
        return taskItems;
    }

    public void addTaskItem(TaskItem newTask) {
        List<TaskItem> currentList = taskItems.getValue();
        if (currentList != null) {
            currentList.add(newTask);
            taskItems.setValue(currentList);
        }
    }

    public void updateTaskItem(TaskItem selectedTask, String taskName, String taskDescription, LocalDate updatedDueDate) {
        // 創建更新後的 TaskItem
        TaskItem updatedTask = new TaskItem(selectedTask.getId(), taskName, taskDescription, updatedDueDate);

        // 在 ViewModel 中找到選定的任務，並更新它
        List<TaskItem> currentList = taskItems.getValue();
        if (currentList != null) {
            int index = currentList.indexOf(selectedTask);
            if (index != -1) {
                currentList.set(index, updatedTask);
                taskItems.setValue(currentList);
            }
        }

        // 獲取 Firebase Database 引用
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("tasks");

        // 更新 "tasks" 路徑下的任務
        Map<String, Object> taskMap = updatedTask.toMap();
        databaseReference.child(selectedTask.getId()).setValue(taskMap); // Assuming you have a toMap() method in TaskItem
    }

    // 添加以下方法處理刪除任務
    public void deleteTaskItem(TaskItem taskItem) {
        String taskId = taskItem.getId();

        // 刪除 Firebase 中的任務
        databaseReference.child(taskId).removeValue();

        // 更新本地 ViewModel 中的任務列表
        List<TaskItem> currentTaskItems = taskItems.getValue();
        if (currentTaskItems != null) {
            currentTaskItems.remove(taskItem);
            taskItems.setValue(currentTaskItems);
        }
    }

    private void setupFirebaseListener() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<TaskItem> taskItemList = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Check the data type before casting
                    Object snapshotValue = snapshot.getValue();

                    if (snapshotValue instanceof Map) {
                        // 如果是 Map 對象，則繼續處理
                        Map<String, Object> taskMap = (Map<String, Object>) snapshotValue;
                        TaskItem taskItem = deserializeTaskItem(taskMap);
                        if (taskItem != null) {
                            taskItemList.add(taskItem);
                        }
                    } else if (snapshotValue instanceof String) {
                        // 如果是字符串，你可以根據實際情況進行處理，或者記錄錯誤信息
                        Log.e("FirebaseListener", "Unexpected data type: String");
                    } else {
                        // 其他未處理的數據類型
                        Log.e("FirebaseListener", "Unexpected data type: " + snapshotValue.getClass().getSimpleName());
                    }
                }

                // Update the LiveData with the new list
                taskItems.setValue(taskItemList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle onCancelled if needed
            }
        });
    }

    // Deserialize TaskItem from Map retrieved from Firebase Database
    private TaskItem deserializeTaskItem(Map<String, Object> taskMap) {
        String id = (String) taskMap.get("id");
        String name = (String) taskMap.get("name");
        String desc = (String) taskMap.get("desc");
        Map<String, String> dueDateMap = (Map<String, String>) taskMap.get("dueDate");
        LocalDate dueDate = LocalDateConverter.deserialize(dueDateMap); // Assuming deserialize is defined in LocalDateConverter

        if (id != null && name != null && desc != null && dueDate != null) {
            // Create and return the TaskItem
            return new TaskItem(id, name, desc, dueDate);
        } else {
            // Handle the case where some essential properties are missing
            return null;
        }
    }

    @Override
    protected void onCleared() {
        // 清空 taskItems
        taskItems.postValue(null);
        super.onCleared();
    }
}
