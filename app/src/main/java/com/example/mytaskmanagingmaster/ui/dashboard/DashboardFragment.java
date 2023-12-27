package com.example.mytaskmanagingmaster.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mytaskmanagingmaster.R;
import com.example.mytaskmanagingmaster.databinding.FragmentDashboardBinding;
import com.example.mytaskmanagingmaster.ui.taskItem.TaskItem;
import com.example.mytaskmanagingmaster.ui.taskItem.TaskItemAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private TaskItemAdapter adapter;
    private List<TaskItem> taskItemList;
    private RecyclerView recyclerView;
    private DatabaseReference tasksReference;

    // 保證初次切入該頁面會顯示當前時間的任務列表
    @Override
    public void onResume() {
        super.onResume();
        dashboardViewModel.setSelectedDate(System.currentTimeMillis());
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        initRecyclerView();
        initViewModel();

        CalendarView calendarView = binding.calendarView;
        calendarView.setDate(System.currentTimeMillis());
        calendarView.setOnDateChangeListener((calendarView1, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            dashboardViewModel.setSelectedDate(calendar.getTimeInMillis());
        });

        return root;
    }

    private void initRecyclerView() {
        recyclerView = binding.calendarRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        tasksReference = FirebaseDatabase.getInstance().getReference().child("tasks");
        taskItemList = new ArrayList<>();
        adapter = new TaskItemAdapter(taskItemList, taskItem -> showUpdateTaskDialog(taskItem), taskItem -> deleteTaskItem(taskItem), tasksReference);
        recyclerView.setAdapter(adapter);
    }

    private void initViewModel() {
        dashboardViewModel.getSelectedDateTaskItems().observe(getViewLifecycleOwner(), selectedDateTaskItems -> {
            taskItemList.clear();
            taskItemList.addAll(selectedDateTaskItems);
            adapter.notifyDataSetChanged();
        });
    }

    private void deleteTaskItem(TaskItem taskItem) {
        dashboardViewModel.deleteTaskItem(taskItem);
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
            dashboardViewModel.updateTaskItem(selectedTask, taskName, taskDescription, updatedDueDate);
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

}
