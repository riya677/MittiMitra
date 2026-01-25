package com.mittimitra.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mittimitra.R;
import com.mittimitra.database.entity.CropSchedule;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CropHistoryAdapter extends RecyclerView.Adapter<CropHistoryAdapter.ViewHolder> {
    private final List<CropSchedule> list;

    public CropHistoryAdapter(List<CropSchedule> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CropSchedule item = list.get(position);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(item.timestamp)));

        // Reuse the location field for the Title/Crop Name
        holder.tvLocation.setText("Crop: " + item.cropName);
        
        // Reuse the summary field for details
        String summary = String.format("Plant: %s | Harvest: %s", item.plantingDate, item.harvestDate);
        holder.tvSummary.setText(summary);

        // TODO: Implement click listener to show full details (maybe re-open CropCalendarActivity with data?)
        // For now, it's just a history record.
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLocation, tvDate, tvSummary;
        ViewHolder(View v) {
            super(v);
            tvLocation = v.findViewById(R.id.tv_card_location);
            tvDate = v.findViewById(R.id.tv_card_date);
            tvSummary = v.findViewById(R.id.tv_card_summary);
        }
    }
}
