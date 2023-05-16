package com.polytech.poubelledroid.socialnetflow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.polytech.poubelledroid.R;
import java.util.List;

public class TwitterFeedAdapter extends RecyclerView.Adapter<TwitterFeedAdapter.TweetViewHolder> {

    private final List<Tweet> mTweets;

    public TwitterFeedAdapter(List<Tweet> tweets) {
        this.mTweets = tweets;
    }

    @NonNull
    @Override
    public TweetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.tweet_item, parent, false);
        return new TweetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TweetViewHolder holder, int position) {
        Tweet tweet = mTweets.get(position);
        holder.username.setText(tweet.getUsername());
        Glide.with(holder.profileImage.getContext())
                .load(tweet.getProfileImageUrl())
                .circleCrop()
                .into(holder.profileImage);

        // remove the hashtag from the tweet content
        String contentWithoutHashtag = tweet.getContent().replace("#poubelledroid", "").trim();

        // remove all links starting with https://t.co/
        String[] words = contentWithoutHashtag.split(" ");
        StringBuilder builder = new StringBuilder();

        for (String word : words) {
            if (word.contains("https://t.co/")) continue;
            builder.append(word).append(" ");
        }

        holder.contentTextView.setText(builder.toString().trim());

        LinearLayoutManager layoutManager =
                new LinearLayoutManager(
                        holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false);
        holder.tweetImagesRecyclerView.setLayoutManager(layoutManager);
        TweetImagesAdapter tweetImagesAdapter = new TweetImagesAdapter(tweet.getMediaUrls());
        holder.tweetImagesRecyclerView.setAdapter(tweetImagesAdapter);

        if (tweet.getMediaUrls().isEmpty()) holder.tweetImagesRecyclerView.setVisibility(View.GONE);
        else holder.tweetImagesRecyclerView.setVisibility(View.VISIBLE);

        holder.retweetCount.setText(tweet.getRetweetCount() + " Retweets");
        holder.likeCount.setText(tweet.getLikeCount() + " Likes");

        if (tweet.getMediaUrls().size() > 1) {
            holder.arrow.setVisibility(View.VISIBLE);
        } else {
            holder.arrow.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mTweets.size();
    }

    static class TweetViewHolder extends RecyclerView.ViewHolder {
        TextView contentTextView;
        TextView username;
        ImageView profileImage;
        RecyclerView tweetImagesRecyclerView;
        TextView retweetCount;
        TextView likeCount;
        ImageView arrow;

        public TweetViewHolder(@NonNull View itemView) {
            super(itemView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            username = itemView.findViewById(R.id.username);
            profileImage = itemView.findViewById(R.id.profile_image);
            tweetImagesRecyclerView = itemView.findViewById(R.id.tweet_images_recycler_view);
            retweetCount = itemView.findViewById(R.id.retweet_count);
            likeCount = itemView.findViewById(R.id.like_count);
            arrow = itemView.findViewById(R.id.arrow_to_show_that_there_are_more_images);
        }
    }
}
