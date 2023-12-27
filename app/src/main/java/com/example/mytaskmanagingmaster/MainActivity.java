package com.example.mytaskmanagingmaster;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.DatePicker;

import com.example.mytaskmanagingmaster.ui.home.HomeFragment;
import com.example.mytaskmanagingmaster.ui.taskItem.TaskItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.mytaskmanagingmaster.databinding.ActivityMainBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        addButton.setOnClickListener(view -> {
            // 顯示新增任務的對話方塊
            showAddTaskDialog();
        });

        // 設定 NavigationView 的菜單項點選事件
        binding.navViewDrawer.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                // 處理登出
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MainActivity.this, Login.class);
                startActivity(intent);
                finish();
                return true;
            }
            // 處理其他菜單項點選事件（如果有）
            return false;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_open_drawer) {
            // 處理點選事件，例如打開側邊欄
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            if (drawer.isDrawerOpen(GravityCompat.END)) {
                drawer.closeDrawer(GravityCompat.END);
            } else {
                drawer.openDrawer(GravityCompat.END);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            // 獲取特定使用者的 Firebase Database 任務引用路徑
            DatabaseReference userTasksReference = FirebaseDatabase.getInstance().getReference("tasks").child(userId);
            // 在路徑下新增任務，Firebase 會自動生成唯一的 ID
            String taskId = userTasksReference.push().getKey();
            // Create TaskItem and serialize dueDate
            TaskItem newTask = new TaskItem(taskId, taskName, taskDescription, dueDate);
            userTasksReference.child(taskId).setValue(newTask.toMap());

            // 在 HomeFragment 中新增任務
            HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
            if (homeFragment != null) {
                homeFragment.addTask(taskId, taskName, taskDescription, dueDate);
            }
        } else {
            // 處理未登錄使用者的情況
        }
    }

}