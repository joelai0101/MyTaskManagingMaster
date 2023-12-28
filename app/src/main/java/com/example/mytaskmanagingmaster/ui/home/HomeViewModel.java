package com.example.mytaskmanagingmaster.ui.home;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.example.mytaskmanagingmaster.ui.taskItem.LocalDateConverter;
import com.example.mytaskmanagingmaster.ui.taskItem.TaskItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<List<TaskItem>> taskItems;
    private DatabaseReference databaseReference;

    public HomeViewModel() {
        taskItems = new MutableLiveData<>();
        taskItems.setValue(new ArrayList<>());

        // 初始化 Firebase Database 引用
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseReference = FirebaseDatabase.getInstance().getReference("tasks").child(userId);
        } else {
            // 處理未登錄使用者的情況
            databaseReference = null;
        }

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

    public void updateTaskItem(@NonNull TaskItem selectedTask, String taskName, String taskDescription, LocalDate updatedDueDate) {
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userTasksReference = FirebaseDatabase.getInstance().getReference("tasks").child(userId);
            userTasksReference.child(selectedTask.getId()).setValue(updatedTask.toMap());
        } else {
            // 處理未登錄使用者的情況
        }
    }

    // 添加以下方法處理刪除任務
    public void deleteTaskItem(@NonNull TaskItem taskItem) {
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

    Comparator<TaskItem> taskComparator = new Comparator<TaskItem>() {
        @Override
        public int compare(TaskItem task1, TaskItem task2) {
            // 首先比較截止日期
            LocalDate dueDate1 = task1.getDueDate();
            LocalDate dueDate2 = task2.getDueDate();
            int dateCompare = dueDate1.compareTo(dueDate2);

            if (dateCompare != 0) {
                // 如果日期不同，根據日期排序
                return dateCompare;
            } else {
                // 如果日期相同，比較任務名稱
                String name1 = task1.getName();
                String name2 = task2.getName();
                return name1.compareToIgnoreCase(name2);
            }
        }
    };

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
                // 使用自訂比較器對任務列表進行排序
                Collections.sort(taskItemList, taskComparator);
                // 更新 LiveData，Update the LiveData with the new list
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
