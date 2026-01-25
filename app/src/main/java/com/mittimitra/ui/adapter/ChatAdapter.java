package com.mittimitra.ui.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Html;
import android.text.Spanned;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mittimitra.ChatMessage;
import com.mittimitra.R;

import java.util.List;

/**
 * RecyclerView adapter for displaying chat messages in the Kisan Sahayak chat feature.
 * Supports user messages (right-aligned, green) and bot messages (left-aligned, white).
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    
    private final List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.messageText.setText(parseMarkdown(message.getMessage()));
        
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.messageText.getLayoutParams();

        if (message.getType() == ChatMessage.Type.USER) {
            params.gravity = Gravity.END;
            holder.messageText.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DCF8C6"))); // Soft Green
            holder.messageText.setTextColor(Color.parseColor("#000000"));
        } else {
            params.gravity = Gravity.START;
            holder.messageText.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF"))); // White
            holder.messageText.setTextColor(Color.parseColor("#333333")); // Dark Grey
        }

        holder.messageText.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /**
     * Convert common Markdown patterns to HTML for display in TextView.
     */
    private static Spanned parseMarkdown(String markdown) {
        if (markdown == null) {
            return Html.fromHtml("", Html.FROM_HTML_MODE_COMPACT);
        }

        // Convert common Markdown to HTML tags
        String html = markdown.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        html = html.replaceAll("####\\s*(.*)", "<br><b>$1</b><br>"); // H4
        html = html.replaceAll("###\\s*(.*)", "<br><b>$1</b><br>"); // H3
        html = html.replaceAll("##\\s*(.*)", "<br><b>$1</b><br>"); // H2
        html = html.replaceAll("^-\\s+(.*)", "• $1<br>"); // List start
        html = html.replaceAll("\\n-\\s+(.*)", "<br>• $1"); // List item
        html = html.replace("\n", "<br>"); // Newlines

        return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.chat_message_text);
        }
    }
}
