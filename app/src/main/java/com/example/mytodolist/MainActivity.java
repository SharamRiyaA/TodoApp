package com.example.mytodolist;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private EditText editTextTask;
    private Button buttonAdd, buttonSetDate, buttonSetTime, buttonClearCompleted;
    private ListView listViewTasks, listViewCompletedTasks;
    private TextView textViewCompleted;
    private ArrayList<String> taskList, completedTaskList;
    private ArrayAdapter<String> adapter, completedAdapter;
    private Calendar taskDateTime;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextTask = findViewById(R.id.editTextTask);
        buttonAdd = findViewById(R.id.buttonAdd);
        buttonSetDate = findViewById(R.id.buttonSetDate);
        buttonSetTime = findViewById(R.id.buttonSetTime);
        buttonClearCompleted = findViewById(R.id.buttonClearCompleted);
        listViewTasks = findViewById(R.id.listViewTasks);
        listViewCompletedTasks = findViewById(R.id.listViewCompletedTasks);
        textViewCompleted = findViewById(R.id.textViewCompleted);

        sharedPreferences = getSharedPreferences("ToDoListApp", Context.MODE_PRIVATE);
        taskList = new ArrayList<>(sharedPreferences.getStringSet("tasks", new HashSet<>()));
        completedTaskList = new ArrayList<>(sharedPreferences.getStringSet("completedTasks", new HashSet<>()));

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, taskList);
        completedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, completedTaskList);
        listViewTasks.setAdapter(adapter);
        listViewCompletedTasks.setAdapter(completedAdapter);

        taskDateTime = null;
        updateCompletedTasksVisibility();

        buttonSetDate.setOnClickListener(v -> {
            int year, month, day;
            if (taskDateTime == null) {
                taskDateTime = Calendar.getInstance();
            }
            year = taskDateTime.get(Calendar.YEAR);
            month = taskDateTime.get(Calendar.MONTH);
            day = taskDateTime.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        if (taskDateTime == null) {
                            taskDateTime = Calendar.getInstance();
                        }
                        taskDateTime.set(year1, monthOfYear, dayOfMonth);
                        Toast.makeText(MainActivity.this, "Date set", Toast.LENGTH_SHORT).show();
                    }, year, month, day);
            datePickerDialog.show();
        });

        buttonSetTime.setOnClickListener(v -> {
            int hour, minute;
            if (taskDateTime == null) {
                taskDateTime = Calendar.getInstance();
            }
            hour = taskDateTime.get(Calendar.HOUR_OF_DAY);
            minute = taskDateTime.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
                    (view, hourOfDay, minute1) -> {
                        if (taskDateTime == null) {
                            taskDateTime = Calendar.getInstance();
                        }
                        taskDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        taskDateTime.set(Calendar.MINUTE, minute1);
                        Toast.makeText(MainActivity.this, "Time set", Toast.LENGTH_SHORT).show();
                    }, hour, minute, false); // Set to false for 12-hour format
            timePickerDialog.show();
        });

        buttonAdd.setOnClickListener(v -> {
            String task = editTextTask.getText().toString().trim();
            if (task.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter a task", Toast.LENGTH_SHORT).show();
                return;
            }

            if (taskDateTime == null) {
                Toast.makeText(MainActivity.this, "Please set a date and time for the task", Toast.LENGTH_SHORT).show();
                return;
            }

            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            DateFormat timeFormat = new SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()); // AM/PM format
            String formattedDate = dateFormat.format(taskDateTime.getTime());
            String formattedTime = timeFormat.format(taskDateTime.getTime());

            String taskWithDateTime = task + " (Due: " + formattedDate + ", " + formattedTime + ")";
            taskList.add(taskWithDateTime);
            adapter.notifyDataSetChanged();
            saveData();
            editTextTask.setText("");
            taskDateTime = null; // Reset taskDateTime after adding task
        });

        listViewTasks.setOnItemClickListener((parent, view, position, id) -> {
            editTask(position);
        });

        listViewTasks.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Task Options")
                    .setMessage("What would you like to do with this task?")
                    .setPositiveButton("Complete", (dialog, which) -> {
                        String task = taskList.remove(position);
                        completedTaskList.add(task);
                        adapter.notifyDataSetChanged();
                        completedAdapter.notifyDataSetChanged();
                        saveData();
                        updateCompletedTasksVisibility();
                    })
                    .setNegativeButton("Delete", (dialog, which) -> {
                        taskList.remove(position);
                        adapter.notifyDataSetChanged();
                        saveData();
                    })
                    .show();
            return true;
        });

        buttonClearCompleted.setOnClickListener(v -> {
            completedTaskList.clear();
            completedAdapter.notifyDataSetChanged();
            saveData();
            updateCompletedTasksVisibility();
        });
    }

    private void editTask(int position) {
        String task = taskList.get(position);
        EditText editText = new EditText(this);
        editText.setText(task);

        new AlertDialog.Builder(this)
                .setTitle("Edit Task")
                .setView(editText)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTask = editText.getText().toString().trim();
                    if (!newTask.isEmpty()) {
                        taskList.set(position, newTask);
                        adapter.notifyDataSetChanged();
                        saveData();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("tasks", new HashSet<>(taskList));
        editor.putStringSet("completedTasks", new HashSet<>(completedTaskList));
        editor.apply();
    }

    private void updateCompletedTasksVisibility() {
        if (completedTaskList.isEmpty()) {
            textViewCompleted.setVisibility(View.GONE);
            listViewCompletedTasks.setVisibility(View.GONE);
            buttonClearCompleted.setVisibility(View.GONE);
        } else {
            textViewCompleted.setVisibility(View.VISIBLE);
            listViewCompletedTasks.setVisibility(View.VISIBLE);
            buttonClearCompleted.setVisibility(View.VISIBLE);
        }
    }
}
