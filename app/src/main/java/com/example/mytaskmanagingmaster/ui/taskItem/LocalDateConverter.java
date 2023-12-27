package com.example.mytaskmanagingmaster.ui.home;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class LocalDateConverter {
    public static Map<String, String> serialize(LocalDate localDate) {
        Map<String, String> serialized = new HashMap<>();
        serialized.put("year", String.valueOf(localDate.getYear()));
        serialized.put("month", String.valueOf(localDate.getMonthValue()));
        serialized.put("day", String.valueOf(localDate.getDayOfMonth()));
        return serialized;
    }

    public static LocalDate deserialize(Map<String, String> serialized) {
        if (serialized == null) {
            // Handle the case where dueDateMap is null
            return null;
        }

        int year = Integer.parseInt(serialized.get("year"));
        int month = Integer.parseInt(serialized.get("month"));
        int day = Integer.parseInt(serialized.get("day"));

        return LocalDate.of(year, month, day);
    }
}
