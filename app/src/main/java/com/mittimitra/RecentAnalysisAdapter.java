package com.mittimitra;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mittimitra.database.entity.SoilAnalysis;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentAnalysisAdapter extends RecyclerView.Adapter<RecentAnalysisAdapter.ViewHolder> {

    private static final String TAG = "RecentAnalysisAdapter";
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault());

    /** Full dataset — used as source for text filtering. */
    private final List<SoilAnalysis> fullList;
    /** Currently displayed dataset (may be a text-filtered subset of fullList). */
    private final List<SoilAnalysis> list;

    public RecentAnalysisAdapter(List<SoilAnalysis> data) {
        this.fullList = new ArrayList<>(data);
        this.list = new ArrayList<>(data);
    }

    // ── Dataset update methods ────────────────────────────────────────────────

    /**
     * Replace the complete dataset (e.g. after a date-range DB reload).
     * Clears any active text filter.
     */
    public void updateList(List<SoilAnalysis> newData) {
        fullList.clear();
        fullList.addAll(newData);
        list.clear();
        list.addAll(newData);
        notifyDataSetChanged();
    }

    /**
     * Filter the displayed list by matching {@code query} against
     * the location and detected_soil fields embedded in the JSON.
     * Pass null or empty string to reset to the full dataset.
     */
    public void filterByText(String query) {
        list.clear();
        if (query == null || query.trim().isEmpty()) {
            list.addAll(fullList);
        } else {
            String lower = query.trim().toLowerCase(Locale.getDefault());
            for (SoilAnalysis item : fullList) {
                if (matchesQuery(item, lower)) {
                    list.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    private boolean matchesQuery(SoilAnalysis item, String lower) {
        try {
            JSONObject json = new JSONObject(item.soilReportJson);
            String location = json.optString("location", "").toLowerCase(Locale.getDefault());
            String soilType = json.optString("detected_soil", "").toLowerCase(Locale.getDefault());
            return location.contains(lower) || soilType.contains(lower);
        } catch (Exception e) {
            return false;
        }
    }

    // ── Swipe-to-delete helpers ────────────────────────────────────────────────

    /** Returns the item at the given adapter position (filtered list). */
    public SoilAnalysis getItem(int position) {
        return list.get(position);
    }

    /** Removes the item at the given adapter position from both lists. */
    public void removeItem(int position) {
        SoilAnalysis removed = list.remove(position);
        fullList.remove(removed);
        notifyItemRemoved(position);
    }

    /** Restores a previously removed item at the end of both lists. */
    public void restoreItem(SoilAnalysis item) {
        fullList.add(item);
        list.add(item);
        notifyItemInserted(list.size() - 1);
    }

    // ── RecyclerView.Adapter ─────────────────────────────────────────────────

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SoilAnalysis item = list.get(position);

        // 1. Format date
        holder.tvDate.setText(DATE_FORMAT.format(new Date(item.timestamp)));

        // 2. Parse JSON for location & NPK summary
        try {
            JSONObject json = new JSONObject(item.soilReportJson);

            String loc = json.optString("location", "Unknown Location");
            if (loc.contains("(")) {
                loc = loc.substring(0, loc.indexOf("(")).trim();
            }
            holder.tvLocation.setText(loc);

            int n = json.optInt("N");
            int p = json.optInt("P");
            int k = json.optInt("K");
            holder.tvSummary.setText(
                    String.format(Locale.US, "N: %d  |  P: %d  |  K: %d", n, p, k));

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse soil report JSON for id=" + item.analysisId, e);
            holder.tvLocation.setText("Analysis #" + item.analysisId);
            holder.tvSummary.setText(
                    holder.itemView.getContext().getString(R.string.data_error));
        }

        // 3. Click → open full report
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), RecommendationActivity.class);
            intent.putExtra("analysis_id", (int) item.analysisId);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

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
