package com.mittimitra.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.mittimitra.R;
import com.mittimitra.database.entity.PlantHealth;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlantHistoryAdapter extends RecyclerView.Adapter<PlantHistoryAdapter.ViewHolder> {
    private final List<PlantHealth> list;
    private final Context context;

    public PlantHistoryAdapter(Context context, List<PlantHealth> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // We can reuse a similar layout or create a new one. Let's try to reuse item_report_card but add an image if possible.
        // Or better, let's assume we might edit the layout to support an image icon. 
        // For now, we'll use the textual one, but maybe set a compound drawable or just rely on text.
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlantHealth item = list.get(position);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(item.timestamp)));

        holder.tvLocation.setText(item.cropName + " (" + item.healthStatus + ")");
        if ("Healthy".equalsIgnoreCase(item.healthStatus)) {
            holder.tvLocation.setTextColor(ContextCompat.getColor(context, R.color.brand_green));
        } else {
            holder.tvLocation.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
        }

        holder.tvSummary.setText("Issues: " + item.diagnosis);

        // Optional: Load image if we had an ImageView in the layout
        // For now, simple text list.
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocation, tvDate, tvSummary;
        ViewHolder(View v) {
            super(v);
            tvLocation = v.findViewById(R.id.tv_card_location); // We use this for Title
            tvDate = v.findViewById(R.id.tv_card_date);
            tvSummary = v.findViewById(R.id.tv_card_summary);
        }
    }
}
