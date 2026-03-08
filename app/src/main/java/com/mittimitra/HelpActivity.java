package com.mittimitra;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class HelpActivity extends BaseActivity {

    // ── Helpline data ──────────────────────────────────────────────────────────

    private static final List<HelplineItem> HELPLINES = new ArrayList<>();

    static {
        HELPLINES.add(new HelplineItem("PM-Kisan Helpline",            "155261",         "PM-KISAN scheme support & queries"));
        HELPLINES.add(new HelplineItem("Kisan Call Centre",            "1800-180-1551",  "Free 24x7 agricultural advisory"));
        HELPLINES.add(new HelplineItem("National Pest Surveillance",   "1800-200-1000",  "Pest & disease outbreak reporting"));
        HELPLINES.add(new HelplineItem("PM Fasal Bima Yojana",         "14447",          "Crop insurance scheme queries"));
        HELPLINES.add(new HelplineItem("ICAR Helpline",                "1800-425-9798",  "Indian Council of Agricultural Research"));
        HELPLINES.add(new HelplineItem("Soil Health Card Helpline",    "1800-180-1551",  "SHC scheme support"));
        HELPLINES.add(new HelplineItem("Kisan Credit Card (KCC)",      "1800-115-526",   "KCC loan & credit queries"));
        HELPLINES.add(new HelplineItem("Maharashtra Agriculture",       "1800-233-4000",  "Maharashtra state agri helpline"));
        HELPLINES.add(new HelplineItem("Uttar Pradesh Agriculture",    "1800-180-5015",  "UP state agriculture helpline"));
        HELPLINES.add(new HelplineItem("Karnataka Agriculture",         "1800-425-3553",  "Karnataka state helpline"));
        HELPLINES.add(new HelplineItem("Tamil Nadu Agriculture",        "1800-425-7213",  "Tamil Nadu state helpline"));
        HELPLINES.add(new HelplineItem("Andhra Pradesh Agriculture",   "1800-425-2910",  "AP state agriculture helpline"));
        HELPLINES.add(new HelplineItem("Telangana Agriculture",         "1800-425-2910",  "Telangana state helpline"));
        HELPLINES.add(new HelplineItem("Punjab Agriculture",            "1800-180-2117",  "Punjab state helpline"));
        HELPLINES.add(new HelplineItem("Gujarat Agriculture",           "1800-233-0222",  "Gujarat state helpline"));
    }

    // ── Activity lifecycle ─────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.help_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_24);
            if (upArrow != null) {
                upArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
            }
        }

        // Contact buttons
        findViewById(R.id.btn_contact_phone).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + getString(R.string.help_contact_phone)));
            startActivity(intent);
        });

        findViewById(R.id.btn_contact_email).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + getString(R.string.help_contact_email)));
            startActivity(intent);
        });

        // Emergency helplines RecyclerView
        RecyclerView rvHelplines = findViewById(R.id.rv_helplines);
        if (rvHelplines != null) {
            rvHelplines.setLayoutManager(new LinearLayoutManager(this));
            rvHelplines.setAdapter(new HelplineAdapter(HELPLINES));
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Inner classes ──────────────────────────────────────────────────────────

    /** Data model for a single helpline entry. */
    private static class HelplineItem {
        final String name;
        final String number;
        final String description;

        HelplineItem(String name, String number, String description) {
            this.name = name;
            this.number = number;
            this.description = description;
        }
    }

    /** Adapter that renders helpline entries and opens the dialler on click. */
    private static class HelplineAdapter
            extends RecyclerView.Adapter<HelplineAdapter.ViewHolder> {

        private final List<HelplineItem> items;

        HelplineAdapter(List<HelplineItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_helpline, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HelplineItem item = items.get(position);
            holder.tvName.setText(item.name);
            holder.tvDesc.setText(item.description);
            holder.tvNumber.setText(item.number);

            View.OnClickListener dialAction = v -> {
                Intent dial = new Intent(Intent.ACTION_DIAL);
                // Strip formatting characters so ACTION_DIAL receives a clean number
                dial.setData(Uri.parse("tel:" + item.number.replaceAll("[^0-9+]", "")));
                v.getContext().startActivity(dial);
            };
            holder.btnCall.setOnClickListener(dialAction);
            holder.itemView.setOnClickListener(dialAction);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvName, tvDesc, tvNumber;
            final MaterialButton btnCall;

            ViewHolder(View v) {
                super(v);
                tvName   = v.findViewById(R.id.tv_helpline_name);
                tvDesc   = v.findViewById(R.id.tv_helpline_desc);
                tvNumber = v.findViewById(R.id.tv_helpline_number);
                btnCall  = v.findViewById(R.id.btn_helpline_call);
            }
        }
    }
}
