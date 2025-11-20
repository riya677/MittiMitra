package com.mittimitra;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mittimitra.database.entity.SoilAnalysis;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentAnalysisAdapter extends RecyclerView.Adapter<RecentAnalysisAdapter.ViewHolder> {
    private final List<SoilAnalysis> list;

    public RecentAnalysisAdapter(List<SoilAnalysis> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // UPDATED: Uses the new card layout
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SoilAnalysis item = list.get(position);

        // 1. Format Date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(item.timestamp)));

        // 2. Parse JSON for Location & Summary
        try {
            JSONObject json = new JSONObject(item.soilReportJson);

            // Location
            String loc = json.optString("location", "Unknown Location");
            // If location has lat/long brackets, remove them for cleaner look
            if (loc.contains("(")) {
                loc = loc.substring(0, loc.indexOf("(")).trim();
            }
            holder.tvLocation.setText(loc);

            // Summary (NPK)
            int n = json.optInt("N");
            int p = json.optInt("P");
            int k = json.optInt("K");
            String summary = String.format(Locale.US, "N: %d  |  P: %d  |  K: %d", n, p, k);
            holder.tvSummary.setText(summary);

        } catch (Exception e) {
            holder.tvLocation.setText("Analysis #" + item.analysisId);
            holder.tvSummary.setText("Data Error");
        }

        // 3. Click to Open Full Report
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), RecommendationActivity.class);
            // Use 'analysisId' which corresponds to the database field
            intent.putExtra("analysis_id", (int)item.analysisId);
            v.getContext().startActivity(intent);
        });
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