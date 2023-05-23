package com.polytech.poubelledroid.socialnetflow;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.polytech.poubelledroid.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;

public class TwitterFeedActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private TwitterFeedAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView noTweetsTextView;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter_feed);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this::fetchTweets);

        noTweetsTextView = findViewById(R.id.no_tweets_text_view);

        AlertDialog.Builder loadingDialogBuilder = new AlertDialog.Builder(this);
        loadingDialogBuilder.setView(R.layout.dialog_loading);
        loadingDialog = loadingDialogBuilder.create();
        loadingDialog.setMessage("Récupération des tweets...");

        fetchTweets();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateNoTweetsVisibility() {
        if (mAdapter == null || mAdapter.getItemCount() == 0) {
            noTweetsTextView.setVisibility(View.VISIBLE);
        } else {
            noTweetsTextView.setVisibility(View.GONE);
        }
    }

    private void fetchTweets() {
        if (!mSwipeRefreshLayout.isRefreshing()) {
            loadingDialog.show();
        }
        new Thread(
                        () -> {
                            try {
                                String searchResponse = TwitterUtils.search();

                                ArrayList<Tweet> tweets = parseTweets(searchResponse);

                                runOnUiThread(
                                        () -> {
                                            mAdapter = new TwitterFeedAdapter(tweets);
                                            mRecyclerView.setAdapter(mAdapter);

                                            if (mSwipeRefreshLayout.isRefreshing()) {
                                                mSwipeRefreshLayout.setRefreshing(false);
                                            }

                                            loadingDialog.dismiss();
                                            updateNoTweetsVisibility();
                                        });

                            } catch (IOException e) {
                                loadingDialog.dismiss();
                                Toast.makeText(
                                                TwitterFeedActivity.this,
                                                "Erreur lors de la récupération des tweets",
                                                Toast.LENGTH_SHORT)
                                        .show();
                                e.printStackTrace();
                            }
                        })
                .start();
    }

    private ArrayList<Tweet> parseTweets(String jsonResponse) {
        ArrayList<Tweet> tweets = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray jsonArray = jsonObject.getJSONArray("data");
            JSONObject includes = jsonObject.getJSONObject("includes");
            JSONArray usersArray = includes.getJSONArray("users");
            JSONArray mediaArray = new JSONArray();
            try {
                mediaArray = includes.getJSONArray("media");
            } catch (Exception ignored) {
            }

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject tweetObject = jsonArray.getJSONObject(i);
                String text = tweetObject.getString("text");
                String authorId = tweetObject.getString("author_id");
                String createdAt = tweetObject.getString("created_at");

                String username = "";
                String profileImageUrl = "";
                List<String> mediaUrls = new ArrayList<>();

                for (int j = 0; j < usersArray.length(); j++) {
                    JSONObject userObject = usersArray.getJSONObject(j);
                    if (authorId.equals(userObject.getString("id"))) {
                        username = userObject.getString("username");
                        profileImageUrl = TwitterUtils.getProfilePic(authorId);
                        break;
                    }
                }

                if (tweetObject.has("attachments")) {
                    JSONObject attachments = tweetObject.getJSONObject("attachments");
                    if (attachments.has("media_keys")) {
                        JSONArray mediaKeys = attachments.getJSONArray("media_keys");
                        mediaUrls = getMediaUrls(mediaKeys, mediaArray);
                    }
                }

                JSONObject publicMetrics = tweetObject.getJSONObject("public_metrics");
                int retweetCount = publicMetrics.getInt("retweet_count");
                int likeCount = publicMetrics.getInt("like_count");

                Tweet tweet =
                        TweetFactory.createTweet(
                                text,
                                username,
                                profileImageUrl,
                                createdAt,
                                mediaUrls,
                                retweetCount,
                                likeCount);
                tweets.add(tweet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tweets;
    }

    private List<String> getMediaUrls(JSONArray mediaKeys, JSONArray mediaArray) {
        List<String> mediaUrls = new ArrayList<>();

        try {
            for (int k = 0; k < mediaKeys.length(); k++) {
                String mediaKey = mediaKeys.getString(k);

                for (int m = 0; m < mediaArray.length(); m++) {
                    JSONObject mediaObject = mediaArray.getJSONObject(m);
                    if (mediaKey.equals(mediaObject.getString("media_key"))) {
                        String mediaUrl = mediaObject.getString("url");
                        mediaUrls.add(mediaUrl);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mediaUrls;
    }
}
