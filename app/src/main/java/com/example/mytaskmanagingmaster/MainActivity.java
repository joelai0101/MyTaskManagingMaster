package com.example.mytaskmanagingmaster;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.DatePicker;

import com.example.mytaskmanagingmaster.ui.home.HomeFragment;
import com.example.mytaskmanagingmaster.ui.home.TaskItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.mytaskmanagingmaster.databinding.ActivityMainBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize Firebase Database's persistence layer
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化 HomeFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment_activity_main, HomeFragment.class, null)
                    .commit();
        }

        // 獲取 FloatingActionButton（假設您的 FloatingActionButton 的 ID 是 fab）
        FloatingActionButton addButton = findViewById(R.id.fab);

        // 設定 FloatingActionButton 的點選事件
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 顯示新增任務的對話方塊
                showAddTaskDialog();
            }
        });

        FloatingActionButton logoutButton = findViewById(R.id.logout);

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);


    }


    // 顯示新增任務的對話方塊
    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Task");

        // 使用自訂佈局
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        final EditText inputTaskName = viewInflated.findViewById(R.id.inputTaskName);
        final EditText inputTaskDescription = viewInflated.findViewById(R.id.inputTaskDescription);
        DatePicker dueDate = viewInflated.findViewById(R.id.inputDueDate);

        // Set default value for dueDate (today)
        LocalDate today = LocalDate.now();
        dueDate.init(today.getYear(), today.getMonthValue() - 1, today.getDayOfMonth(), null);

        builder.setView(viewInflated);

        // 設定對話方塊按鈕
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            // 獲取輸入的任務名稱和描述
            String taskName = inputTaskName.getText().toString().trim();
            String taskDescription = inputTaskDescription.getText().toString().trim();
            LocalDate taskDueDate = LocalDate.of(dueDate.getYear(), dueDate.getMonth() + 1, dueDate.getDayOfMonth());

            // 將任務資訊新增到 Firebase Database
            addTask(taskName, taskDescription, taskDueDate);
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addTask(String taskName, String taskDescription, LocalDate dueDate) {
        // 獲取 Firebase Database 引用
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("tasks");

        // 在 "tasks" 路徑下新增任務，Firebase 會自動生成唯一的 ID
        String taskId = databaseReference.push().getKey();

        // Create TaskItem and serialize dueDate
        TaskItem newTask = new TaskItem(taskId, taskName, taskDescription, dueDate);
        Map<String, Object> taskMap = newTask.toMap(); // Assuming you have a toMap() method in TaskItem

        // Set the value in Firebase Database
        databaseReference.child(taskId).setValue(taskMap);

        // 在 HomeFragment 中新增任務
        HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        if (homeFragment != null) {
            homeFragment.addTask(taskId, taskName, taskDescription, dueDate);
        }
    }

}