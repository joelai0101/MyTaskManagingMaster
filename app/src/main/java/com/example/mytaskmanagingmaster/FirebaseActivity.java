package com.example.mytaskmanagingmaster;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;
public class FirebaseActivity extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 在自訂類別中設定 setPersistenceEnabled 而不是在任何其他活動中，
        // 保證無論使用者如何使用應用 setPersistenceEnabled 只在應用啟動時呼叫一次。
        // 要在任何其他 Firebase 操作前呼叫
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
