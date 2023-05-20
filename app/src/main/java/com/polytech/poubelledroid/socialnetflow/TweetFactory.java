package com.polytech.poubelledroid.socialnetflow;

import java.util.List;

public class TweetFactory {

    private TweetFactory() {
        // This class is not meant to be instantiated.
    }

    public static Tweet createTweet(
            String content,
            String username,
            String profileImageUrl,
            String dateAsString,
            List<String> mediasURL,
            int retweetCount,
            int favoriteCount) {
        return new Tweet(
                content,
                username,
                profileImageUrl,
                dateAsString,
                mediasURL,
                retweetCount,
                favoriteCount);
    }
}
