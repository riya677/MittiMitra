package com.mittimitra.ui.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.mittimitra.R;
import com.mittimitra.database.entity.FarmTask;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class FarmTaskAdapter extends RecyclerView.Adapter<FarmTaskAdapter.TaskViewHolder> {

    public interface TaskActionListener {
        void onMarkComplete(FarmTask task);
    }

    private List<FarmTask> tasks;
    private final TaskActionListener listener;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public FarmTaskAdapter(List<FarmTask> tasks, TaskActionListener listener) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.listener = listener;
    }

    public void update(List<FarmTask> newTasks) {
        this.tasks = newTasks != null ? newTasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_farm_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        if (tasks == null || position < 0 || position >= tasks.size()) {
            return;
        }
        FarmTask task = tasks.get(position);

        holder.tvTitle.setText(task.title);
        holder.tvDescription.setText(task.description == null || task.description.isEmpty()
                ? holder.itemView.getContext().getString(R.string.task_no_description)
                : task.description);
        holder.tvMeta.setText(holder.itemView.getContext().getString(
                R.string.task_meta_format,
                task.cropName == null || task.cropName.isEmpty() ? holder.itemView.getContext().getString(R.string.status_none) : task.cropName,
                DATE_FORMAT.format(new java.util.Date(task.dueAt))
        ));

        boolean completed = "COMPLETED".equalsIgnoreCase(task.status);
        holder.tvTitle.setPaintFlags(completed
                ? holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        holder.btnComplete.setVisibility(completed ? View.GONE : View.VISIBLE);
        holder.btnComplete.setOnClickListener(v -> listener.onMarkComplete(task));
    }

    @Override
    public int getItemCount() {
        return tasks == null ? 0 : tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvDescription;
        TextView tvMeta;
        MaterialButton btnComplete;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDescription = itemView.findViewById(R.id.tv_task_description);
            tvMeta = itemView.findViewById(R.id.tv_task_meta);
            btnComplete = itemView.findViewById(R.id.btn_task_complete);
        }
    }
}
