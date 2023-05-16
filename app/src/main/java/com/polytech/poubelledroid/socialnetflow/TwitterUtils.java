package com.polytech.poubelledroid.socialnetflow;

import com.polytech.poubelledroid.BuildConfig;
import java.io.IOException;
import java.util.Objects;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class TwitterUtils {

    public static final String DEFAULT_QUERY = "#poubelledroid";

    private TwitterUtils() {}

    private static final String BEARER_TOKEN = BuildConfig.TWITTER_BEARER_TOKEN;

    public static String search() throws IOException {
        OkHttpClient httpClient = new OkHttpClient();

        String expansions = "author_id,attachments.media_keys";
        String tweetFields = "created_at,attachments,public_metrics";
        String userFields = "username";
        String mediaFields = "url";

        HttpUrl.Builder httpBuilder =
                Objects.requireNonNull(
                                HttpUrl.parse("https://api.twitter.com/2/tweets/search/recent"))
                        .newBuilder()
                        .addQueryParameter("query", DEFAULT_QUERY)
                        .addQueryParameter(
                                "max_results", "10") // Because I do not wish to get rate limited
                        .addQueryParameter("expansions", expansions)
                        .addQueryParameter("tweet.fields", tweetFields)
                        .addQueryParameter("user.fields", userFields)
                        .addQueryParameter("media.fields", mediaFields);

        Request request =
                new Request.Builder()
                        .url(httpBuilder.build())
                        .addHeader(
                                "Authorization",
                                String.format("Bearer %s", TwitterUtils.BEARER_TOKEN))
                        .addHeader("Content-Type", "application/json")
                        .build();

        try (Response response = httpClient.newCall(request).execute()) {
            assert response.body() != null;
            return response.body().string();
        }
    }

    public static String getProfilePic(String userId) throws IOException {
        OkHttpClient httpClient = new OkHttpClient();

        HttpUrl.Builder httpBuilder =
                Objects.requireNonNull(HttpUrl.parse("https://api.twitter.com/2/users/" + userId))
                        .newBuilder()
                        .addQueryParameter("user.fields", "profile_image_url");

        Request request =
                new Request.Builder()
                        .url(httpBuilder.build())
                        .addHeader("Authorization", String.format("Bearer %s", BEARER_TOKEN))
                        .addHeader("Content-Type", "application/json")
                        .build();

        try (Response response = httpClient.newCall(request).execute()) {
            assert response.body() != null;
            String jsonResponse = response.body().string();
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject userObject = jsonObject.getJSONObject("data");
            return userObject.getString("profile_image_url");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
