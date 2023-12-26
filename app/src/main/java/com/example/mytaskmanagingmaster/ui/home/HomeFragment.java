package com.example.mytaskmanagingmaster.ui.home;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mytaskmanagingmaster.R;
import com.example.mytaskmanagingmaster.databinding.FragmentHomeBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private TaskItemAdapter adapter;
    private List<TaskItem> taskItemList;
    private RecyclerView recyclerView;
    private DatabaseReference tasksReference; // 添加 DatabaseReference

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        initRecyclerView();

        return root;
    }

    private void initRecyclerView() {
        recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // 初始化 tasksReference
        tasksReference = FirebaseDatabase.getInstance().getReference().child("tasks");

        taskItemList = new ArrayList<>();
        // 將 tasksReference 傳入 TaskItemAdapter
        adapter = new TaskItemAdapter(taskItemList, taskItem -> {
            // Handle task item click by showing update dialog
            showUpdateTaskDialog(taskItem);
        }, taskItem -> {
            // Handle task item checkbox click for deletion
            deleteTaskItem(taskItem);
        }, tasksReference);

        recyclerView.setAdapter(adapter);

        homeViewModel.getTaskItems().observe(getViewLifecycleOwner(), taskItems -> {
            taskItemList.clear();
            taskItemList.addAll(taskItems);
            adapter.notifyDataSetChanged();
        });

    }

    // New method to add a task to the ViewModel
    public void addTask(String taskId, String taskName, String taskDescription, LocalDate dueDate) {
        // Create a new TaskItem with the given name and description
        TaskItem newTask = new TaskItem(taskId, taskName, taskDescription, dueDate);

        // Add the new task to the ViewModel
        homeViewModel.addTaskItem(newTask);
    }

    private void deleteTaskItem(TaskItem taskItem) {
        // 請 HomeViewModel 刪除任務
        homeViewModel.deleteTaskItem(taskItem);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showUpdateTaskDialog(TaskItem selectedTask) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Update Task");

        // 使用自訂佈局
        View viewInflated = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update_task, null);
        final EditText inputTaskName = viewInflated.findViewById(R.id.updateTaskName);
        final EditText inputTaskDescription = viewInflated.findViewById(R.id.updateTaskDescription);
        DatePicker dueDate = viewInflated.findViewById(R.id.updateDueDate);

        // 將選定的任務資訊設置到對話方塊中
        inputTaskName.setText(selectedTask.getName());
        inputTaskDescription.setText(selectedTask.getDesc());
        LocalDate taskDueDate = selectedTask.getDueDate();
        dueDate.init(taskDueDate.getYear(), taskDueDate.getMonthValue() - 1, taskDueDate.getDayOfMonth(), null);

        builder.setView(viewInflated);

        // 設定對話方塊按鈕
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            // 獲取輸入的任務名稱和描述
            String taskName = inputTaskName.getText().toString().trim();
            String taskDescription = inputTaskDescription.getText().toString().trim();
            LocalDate updatedDueDate = LocalDate.of(dueDate.getYear(), dueDate.getMonth() + 1, dueDate.getDayOfMonth());

            // 請 HomeViewModel 更新任務資訊
            homeViewModel.updateTaskItem(selectedTask, taskName, taskDescription, updatedDueDate);
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
