package com.example.mytaskmanagingmaster.ui;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;
public class FirebaseActivity extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 在任何其他 Firebase 操作前呼叫
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
