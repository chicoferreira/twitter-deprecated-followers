package com.github.chicoferreira.twitterdeprecatedfollowers;

import com.twitter.clientlib.ApiException;
import com.twitter.clientlib.TwitterCredentialsBearer;
import com.twitter.clientlib.api.TwitterApi;
import com.twitter.clientlib.model.Tweet;
import com.twitter.clientlib.model.User;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class Main {

    private static final String PATTERN = "dd/MM/yyyy HH:mm:ss";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN);

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Provide the target user name @ (without @) as argument:");
            System.err.println("java -jar twitter-deprecated-followers.jar <target user here>");
            return;
        }

        String targetUser = args[0];

        if (!targetUser.matches("^[A-Za-z0-9_]{1,15}")) {
            System.err.println("Invalid user name '" + targetUser + "'. User name must be between 1 and 15 characters long and can contain only letters, numbers and underscores.");
            return;
        }

        String credentials = loadCredentialsFromFile("credentials.txt");
        if (credentials == null) {
            System.err.println("credentials.txt not found. Please create it and paste your twitter app Bearer token (Create an app here https://developer.twitter.com/en/portal/dashboard).");
            return;
        }

        TwitterCredentialsBearer bearer = new TwitterCredentialsBearer(credentials);
        TwitterApi twitterApi = new TwitterApi(bearer);

        List<UserData> userData = new ArrayList<>();

        try {
            var userRequest = twitterApi.users().findUserByUsername(targetUser)
                                        .userFields(Set.of("public_metrics", "protected"))
                                        .execute();


            User user = userRequest.getData();

            if (user == null) {
                System.err.println("User '" + targetUser + "' is not registered on Twitter");
                return;
            }

            String id = user.getId();
            String name = user.getName();
            String username = user.getUsername();

            System.out.println("Found user @" + username + " (" + name + ") with id " + id);
            System.out.println("Following " + user.getPublicMetrics().getFollowingCount() + " accounts");

            List<User> followers = new ArrayList<>();

            String nextPaginatedCursor = null;
            do {
                var followingRequest = twitterApi.users().usersIdFollowing(id)
//                                                 .maxResults(5)
                                                 .paginationToken(nextPaginatedCursor)
                                                 .userFields(Set.of("public_metrics", "protected"))
                                                 .execute();
                followers.addAll(followingRequest.getData());
                nextPaginatedCursor = followingRequest.getMeta().getNextToken();
            } while (nextPaginatedCursor != null);

            System.out.println();

            int current = 1;
            int size = followers.size();

            for (User follower : followers) {
                String followerId = follower.getId();
                String followerName = follower.getName();
                String followerUsername = follower.getUsername();


                List<Tweet> data = twitterApi.tweets().usersIdTweets(followerId)
                                             .tweetFields(Set.of("created_at"))
                                             .maxResults(5) // TwitterAPI does not allow less than 5 tweets fetched
                                             .execute()
                                             .getData();

                Tweet tweet = null;
                if (!follower.getProtected() && data != null && !data.isEmpty()) {
                    tweet = data.get(0);
                }

                userData.add(new UserData(follower, tweet, follower.getProtected()));
                System.out.print("\r(" + current + "/" + size + ") Fetching @" + followerUsername + " (" + followerName + ") with id " + followerId + " https://twitter.com/" + followerUsername);
                current++;
            }
        } catch (ApiException e) {
            e.printStackTrace();
        }

        System.out.print("\rFetching complete.\n\n");
        System.out.println("Sorting user data by last tweet date...");

        Comparator<UserData> comparator = (a, b) -> {
            if (a.lastTweet == null && b.lastTweet == null) {
                return a.isProtectedUser ? 1 : -1;
            } else if (a.lastTweet == null) {
                return -1;
            } else if (b.lastTweet == null) {
                return 1;
            } else {
                return a.lastTweet.getCreatedAt().compareTo(b.lastTweet.getCreatedAt());
            }
        };

        userData.sort(comparator);
        System.out.println();

        System.out.println("Last tweet data:");
        for (UserData user : userData) {
            Tweet tweet = user.lastTweet();
            User twitterUser = user.user();
            if (tweet == null) {
                if (twitterUser.getProtected()) {
                    System.out.println("\t(" + "-".repeat(PATTERN.length()) + ") https://twitter.com/" + twitterUser.getUsername() + " is protected account");
                } else {
                    System.out.println("\t(" + "-".repeat(PATTERN.length()) + ") https://twitter.com/" + twitterUser.getUsername() + " has no tweets");
                }
            } else {
                System.out.println("\t(" + tweet.getCreatedAt().format(TIME_FORMATTER) + ") https://twitter.com/" + twitterUser.getUsername() + " last tweet:" + " https://twitter.com/" + twitterUser.getUsername() + "/status/" + tweet.getId());
            }
        }
    }

    public static String loadCredentialsFromFile(String filePath) {
        URL location = Main.class.getProtectionDomain().getCodeSource().getLocation();
        String codeLocation = location.toString();

        Path path = null;

        if (codeLocation.endsWith(".jar")) {
            System.out.println("Loading credentials from " + filePath + " in jar file folder");
            path = Path.of(location.getPath()).getParent().resolve(filePath);
        } else {
            System.out.println("Loading credentials from " + filePath + " in classpath");

            URL url = Main.class.getClassLoader().getResource(filePath);
            if (url != null) path = Path.of(url.getPath());
        }

        try {
            return path != null ? Files.readString(path) : null;
        } catch (IOException e) {
            return null;
        }
    }

    public record UserData(User user, Tweet lastTweet, boolean isProtectedUser) {
    }

}