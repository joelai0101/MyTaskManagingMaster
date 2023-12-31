package com.example.mytaskmanagingmaster.ui.dashboard;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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

public class DashboardViewModel extends ViewModel {

    private final MutableLiveData<List<TaskItem>> taskItems;
    // 在 DashboardViewModel 中新增一個 LiveData 用於存儲所選日期的任務列表
    private MutableLiveData<List<TaskItem>> selectedDateTaskItems;
    private MutableLiveData<Long> selectedDate = new MutableLiveData<>();
    private DatabaseReference databaseReference;


    public DashboardViewModel() {
        taskItems = new MutableLiveData<>();
        taskItems.setValue(new ArrayList<>());
        selectedDateTaskItems = new MutableLiveData<>();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // 初始化 Firebase Database 引用
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
                List<TaskItem> allTaskItemList = new ArrayList<>(); // 全域任務列表
                List<TaskItem> selectedDateTaskItemList = new ArrayList<>(); // 選定日期的任務列表
                LocalDate selectedLocalDate = selectedDate.getValue() != null ?
                        Instant.ofEpochMilli(selectedDate.getValue()).atZone(ZoneId.systemDefault()).toLocalDate() : null;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Check the data type before casting
                    Object snapshotValue = snapshot.getValue();

                    if (snapshotValue instanceof Map) {
                        // 如果是 Map 對象，則繼續處理
                        Map<String, Object> taskMap = (Map<String, Object>) snapshotValue;
                        TaskItem taskItem = deserializeTaskItem(taskMap);
                        if (allTaskItemList != null) {
                            allTaskItemList.add(taskItem);
                        }
                        // 檢查任務是否屬於當前選定的日期
                        if (selectedDate.getValue() != null) {
                            if (taskItem.getDueDate() != null && taskItem.getDueDate().isEqual(selectedLocalDate)) {
                                selectedDateTaskItemList.add(taskItem); // 選定日期的任務列表需要在資料更動時更新，否則更新後只會顯示全域任務
                            }
                        }
                    } else if (snapshotValue instanceof String) {
                        // 如果是字符串，可以根據實際情況進行處理，或者記錄錯誤信息
                        Log.e("FirebaseListener", "Unexpected data type: String");
                    } else {
                        // 其他未處理的數據類型
                        Log.e("FirebaseListener", "Unexpected data type: " + snapshotValue.getClass().getSimpleName());
                    }
                }
                // 使用自訂比較器對任務列表進行排序
                Collections.sort(allTaskItemList, taskComparator);
                Collections.sort(selectedDateTaskItemList, taskComparator);
                // 更新全域和選定日期的任務列表
                taskItems.setValue(allTaskItemList);
                selectedDateTaskItems.setValue(selectedDateTaskItemList);
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

    // 新增一個方法以獲取所選日期的任務列表
    public LiveData<List<TaskItem>> getSelectedDateTaskItems() {
        return selectedDateTaskItems;
    }

    // 更新所選日期的任務列表
    public void updateSelectedDateTaskItems(LocalDate selectedDate) {
        List<TaskItem> allTasks = taskItems.getValue();
        List<TaskItem> filteredList = new ArrayList<>();

        if (selectedDate == null) {
            // 處理 selectedDate 為 null 的情況
            selectedDateTaskItems.setValue(Collections.emptyList());
            return;
        }

        if (allTasks != null) {
            // 遍歷所有任務，篩選出指定日期的任務
            for (TaskItem taskItem : allTasks) {
                if (taskItem.getDueDate().isEqual(selectedDate)) {
                    filteredList.add(taskItem);
                }
            }
        }

        selectedDateTaskItems.setValue(filteredList);
    }

    // 設定所選日期
    public void setSelectedDate(long selectedDateMillis) {
        this.selectedDate.setValue(selectedDateMillis);

        // 直接使用毫秒數創建 LocalDate 對象，避免時區的問題導致日期顯示錯誤
        LocalDate selectedLocalDate = Instant.ofEpochMilli(selectedDateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // 調用新的方法以更新所選日期的任務列表
        updateSelectedDateTaskItems(selectedLocalDate);
    }


    @Override
    protected void onCleared() {
        // 清空 taskItems
        taskItems.postValue(null);
        super.onCleared();
    }
}
