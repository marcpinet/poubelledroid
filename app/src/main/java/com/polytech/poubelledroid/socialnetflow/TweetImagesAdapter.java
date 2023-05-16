package com.polytech.poubelledroid.socialnetflow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.polytech.poubelledroid.R;
import java.util.List;

public class TweetImagesAdapter extends RecyclerView.Adapter<TweetImagesAdapter.ImageViewHolder> {
    private final List<String> imageUrls;

    public TweetImagesAdapter(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.tweet_image_item, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        if (imageUrls.isEmpty()) return;

        String imageUrl = imageUrls.get(position);
        Glide.with(holder.tweetImage.getContext()).load(imageUrl).into(holder.tweetImage);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView tweetImage;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            tweetImage = itemView.findViewById(R.id.tweet_image);
        }
    }
}
