package com.mittimitra;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mittimitra.data.repository.RoomTaskRepository;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.FarmTask;
import com.mittimitra.database.entity.TaskReminder;
import com.mittimitra.ui.adapters.FarmTaskAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FarmTaskPlannerActivity extends BaseActivity implements FarmTaskAdapter.TaskActionListener {

    private RecyclerView recyclerTasks;
    private TextView tvEmptyTasks;
    private FarmTaskAdapter adapter;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private RoomTaskRepository taskRepository;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farm_task_planner);

        Toolbar toolbar = findViewById(R.id.toolbar_tasks);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.farm_tasks_title);
        }

        String resolvedUserId = UserIdentityResolver.getActiveUserIdOrCreateGuest(this);

        userId = resolvedUserId;
        taskRepository = new RoomTaskRepository(MittiMitraDatabase.getDatabase(this));

        recyclerTasks = findViewById(R.id.recycler_farm_tasks);
        tvEmptyTasks = findViewById(R.id.tv_empty_tasks);
        FloatingActionButton fabAddTask = findViewById(R.id.fab_add_task);

        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FarmTaskAdapter(new ArrayList<>(), this);
        recyclerTasks.setAdapter(adapter);

        fabAddTask.setOnClickListener(v -> showCreateTaskDialog());

        loadTasks();
    }

    private void loadTasks() {
        executor.execute(() -> {
            try {
                List<FarmTask> tasks = taskRepository.getTasksForUser(userId);
                runOnUiThread(() -> {
                    adapter.update(tasks);
                    boolean hasItems = tasks != null && !tasks.isEmpty();
                    recyclerTasks.setVisibility(hasItems ? View.VISIBLE : View.GONE);
                    tvEmptyTasks.setVisibility(hasItems ? View.GONE : View.VISIBLE);
                });
            } catch (Exception e) {
                android.util.Log.e("FarmTaskPlanner", "Failed to load tasks", e);
                runOnUiThread(() -> Toast.makeText(this, R.string.alert_data_unavailable, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showCreateTaskDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_farm_task, null);
        TextInputEditText etTitle = view.findViewById(R.id.et_task_title);
        TextInputEditText etDescription = view.findViewById(R.id.et_task_description);
        TextInputEditText etCrop = view.findViewById(R.id.et_task_crop);
        TextInputEditText etDueDate = view.findViewById(R.id.et_task_due_date);

        final long[] selectedDueAt = {System.currentTimeMillis() + (24L * 60L * 60L * 1000L)};
        etDueDate.setOnClickListener(v -> openDatePicker(etDueDate, selectedDueAt));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.add_task_title)
                .setView(view)
                .setPositiveButton(R.string.dialog_save, null)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = text(etTitle);
            if (title.isEmpty()) {
                etTitle.setError(getString(R.string.task_title_required));
                return;
            }

            FarmTask task = new FarmTask();
            task.userId = userId;
            task.title = title;
            task.description = text(etDescription);
            task.cropName = text(etCrop);
            task.stage = "manual";
            task.source = "manual";
            task.dueAt = selectedDueAt[0];
            task.status = "PENDING";
            task.priority = 2;
            task.createdAt = System.currentTimeMillis();

            executor.execute(() -> {
                long taskId = taskRepository.createTask(task);
                TaskReminder reminder = new TaskReminder();
                reminder.taskId = taskId;
                reminder.userId = userId;
                reminder.remindAt = Math.max(task.dueAt - (2L * 60L * 60L * 1000L), System.currentTimeMillis() + 60000);
                reminder.channel = "app_notification";
                reminder.isSent = false;
                reminder.createdAt = System.currentTimeMillis();
                taskRepository.upsertReminder(reminder);

                runOnUiThread(() -> {
                    dialog.dismiss();
                    loadTasks();
                    Toast.makeText(this, R.string.task_saved, Toast.LENGTH_SHORT).show();
                });
            });
        }));

        dialog.show();
    }

    private void openDatePicker(TextInputEditText etDueDate, long[] selectedDueAt) {
        Calendar c = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(this, (DatePicker view, int year, int month, int day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(Calendar.YEAR, year);
            selected.set(Calendar.MONTH, month);
            selected.set(Calendar.DAY_OF_MONTH, day);
            selected.set(Calendar.HOUR_OF_DAY, 9);
            selected.set(Calendar.MINUTE, 0);
            selectedDueAt[0] = selected.getTimeInMillis();

            String dateText = String.format(java.util.Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            etDueDate.setText(dateText);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        picker.show();
    }

    private String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    @Override
    public void onMarkComplete(FarmTask task) {
        executor.execute(() -> {
            try {
                taskRepository.completeTask(task.id, System.currentTimeMillis());
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.task_completed, Toast.LENGTH_SHORT).show();
                    loadTasks();
                });
            } catch (Exception e) {
                android.util.Log.e("FarmTaskPlanner", "Failed to mark task complete", e);
                runOnUiThread(() -> Toast.makeText(this, R.string.alert_data_unavailable, Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
