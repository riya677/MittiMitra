package com.mittimitra;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder> {
    private final List<Integer> images;
    private final List<String> titles;

    public CarouselAdapter(List<Integer> images, List<String> titles) {
        this.images = images;
        this.titles = titles;
    }

    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_carousel, parent, false);
        return new CarouselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        holder.imageView.setImageResource(images.get(position));
        holder.tvTitle.setText(titles.get(position));
    }

    @Override
    public int getItemCount() { return images.size(); }

    static class CarouselViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvTitle;
        CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.img_carousel);
            tvTitle = itemView.findViewById(R.id.tv_carousel_title);
        }
    }
}