package com.example.mytaskmanagingmaster.ui.taskItem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mytaskmanagingmaster.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class TaskItemAdapter extends RecyclerView.Adapter<TaskItemAdapter.TaskItemViewHolder> {

    private final List<TaskItem> taskItemList;
    private final TaskItemClickListener clickListener;
    private final TaskItemCheckboxClickListener checkboxClickListener;

    private final DatabaseReference tasksReference;
    public TaskItemAdapter(List<TaskItem> taskItemList, TaskItemClickListener clickListener, TaskItemCheckboxClickListener checkboxClickListener, DatabaseReference tasksReference) {
        this.taskItemList = taskItemList;
        this.clickListener = clickListener;
        this.checkboxClickListener = checkboxClickListener;
        this.tasksReference = tasksReference;
    }

    @NonNull
    @Override
    public TaskItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_task_list_item, parent, false);
        return new TaskItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskItemViewHolder holder, int position) {
        // 繫結 TaskItem 的資料到 ViewHolder 中的元素
        holder.bindTaskItem(taskItemList.get(position));

        // 設置點擊事件
        holder.itemView.setOnClickListener(view -> {
            if (clickListener != null) {
                clickListener.onTaskItemClick(taskItemList.get(position));
            }
        });

        // 設置 CheckBox 的預設狀態為 false
        holder.checkbox.setChecked(false);
    }

    @Override
    public int getItemCount() {
        return taskItemList.size();
    }

    public class TaskItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final TextView textTaskName;
        private final TextView textTaskDescription;
        private final TextView textDueDate;
        private final CheckBox checkbox;
        private final DatabaseReference tasksReference; // 添加 DatabaseReference
        public TaskItemViewHolder(@NonNull View itemView) {
            super(itemView);

            textTaskName = itemView.findViewById(R.id.textTaskName);
            textTaskDescription = itemView.findViewById(R.id.textTaskDescription);
            textDueDate = itemView.findViewById(R.id.textDueDate);

            checkbox = itemView.findViewById(R.id.checkbox); // 初始化 checkbox
            checkbox.setOnClickListener(this); // 設置 Checkbox 的點擊事件
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                tasksReference = FirebaseDatabase.getInstance().getReference().child("tasks").child(userId);
            } else {
                // 處理未登錄使用者的情況
                tasksReference = null;
            }

        }

        public void bindTaskItem(TaskItem taskItem) {
            textTaskName.setText(taskItem.getName());
            textTaskDescription.setText(taskItem.getDesc());

            // Format due date if available
            if (taskItem.getDueDate() != null) {
                textDueDate.setText("Due: " + taskItem.getDueDate().toString());
            } else {
                textDueDate.setText("");
            }
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.checkbox && checkboxClickListener != null) {
                checkboxClickListener.onTaskItemCheckboxClick(taskItemList.get(getAdapterPosition()));
            } else {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onTaskItemClick(taskItemList.get(position));
                }
            }
        }
    }
}
