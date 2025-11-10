package com.mittimitra;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.mittimitra.database.entity.Document;
import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private final List<Document> documentList;
    private final OnDocumentClickListener listener;

    // Interface for handling clicks
    public interface OnDocumentClickListener {
        void onViewClick(Document document);
        void onDeleteClick(Document document);
    }

    public DocumentAdapter(List<Document> documentList, OnDocumentClickListener listener) {
        this.documentList = documentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        Document document = documentList.get(position);
        holder.bind(document, listener);
    }

    @Override
    public int getItemCount() {
        return documentList.size();
    }

    // ViewHolder class
    static class DocumentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDocName;
        private final MaterialButton btnViewDoc;
        private final ImageButton btnDeleteDoc;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDocName = itemView.findViewById(R.id.tv_doc_name);
            btnViewDoc = itemView.findViewById(R.id.btn_view_doc);
            btnDeleteDoc = itemView.findViewById(R.id.btn_delete_doc);
        }

        public void bind(final Document document, final OnDocumentClickListener listener) {
            tvDocName.setText(document.documentName);

            btnViewDoc.setOnClickListener(v -> listener.onViewClick(document));
            btnDeleteDoc.setOnClickListener(v -> listener.onDeleteClick(document));
        }
    }
}