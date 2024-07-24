package com.youtube.service;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;

@Service
public class YouTubeService {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeService.class);

    @Value("${youtube.api.key}")
    private String apiKey;
    private static final String APPLICATION_NAME = "youtube subscriber";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private YouTube getYouTubeService() throws GeneralSecurityException, IOException {
        return new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                null)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public Channel getChannelInfoByChannelName(String channelName) throws GeneralSecurityException, IOException {
        logger.info("Fetching channel info for channel name: {}", channelName);

        String encodedChannelName = URLEncoder.encode(channelName, StandardCharsets.UTF_8);

        YouTube youtubeService = getYouTubeService();

        YouTube.Channels.List request = youtubeService.channels()
                .list("statistics")
                .setKey(apiKey)
                .setForUsername(encodedChannelName);

        ChannelListResponse response = request.execute();

        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            logger.error("No channel found with the provided name: {}", channelName);
            throw new IOException("No channel found with the provided name.");
        }

        Channel channel = response.getItems().get(0);
        logger.info("Channel info retrieved: {}", channel.toPrettyString());

        return channel;
    }

    public BigInteger getLiveViewersCountByChannelName(String channelName) throws GeneralSecurityException, IOException {
        Channel channel = getChannelInfoByChannelName(channelName);

        if (channel == null) {
            logger.error("Channel info is null for channel name: {}", channelName);
            throw new IOException("Channel info is null for channel name.");
        }

        String channelId = channel.getId();

        logger.info("Checking if channel is live for channel ID: {}", channelId);

        YouTube youtubeService = getYouTubeService();

        YouTube.Search.List searchRequest = youtubeService.search()
                .list("id")
                .setKey(apiKey)
                .setChannelId(channelId)
                .setEventType("live")
                .setType("video");

        SearchListResponse searchResponse = searchRequest.execute();
        List<SearchResult> searchResults = searchResponse.getItems();

        if (searchResults == null || searchResults.isEmpty()) {
            logger.info("Channel is not live.");
            return BigInteger.ZERO; // Return zero viewers if not live
        }

        String liveVideoId = searchResults.get(0).getId().getVideoId();
        logger.info("Live video ID: {}", liveVideoId);

        YouTube.Videos.List videoRequest = youtubeService.videos()
                .list("liveStreamingDetails")
                .setKey(apiKey)
                .setId(liveVideoId);

        VideoListResponse videoResponse = videoRequest.execute();
        List<Video> videos = videoResponse.getItems();

        if (videos == null || videos.isEmpty()) {
            logger.error("No live video details found.");
            return BigInteger.ZERO; // Return zero viewers if no live details found
        }

        VideoLiveStreamingDetails liveStreamingDetails = videos.get(0).getLiveStreamingDetails();
        BigInteger concurrentViewers = liveStreamingDetails.getConcurrentViewers();
        logger.info("Live viewers: {}", concurrentViewers);

        return concurrentViewers;
    }

    public Video getVideoDetails(String videoId) throws GeneralSecurityException, IOException {
        logger.info("Fetching video details for video ID: {}", videoId);

        YouTube youtubeService = getYouTubeService();

        YouTube.Videos.List request = youtubeService.videos()
                .list("snippet,statistics,contentDetails")
                .setKey(apiKey)
                .setId(videoId);

        VideoListResponse response = request.execute();

        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            logger.error("No video found with the provided ID: {}", videoId);
            throw new IOException("No video found with the provided ID.");
        }

        Video video = response.getItems().get(0);
        logger.info("Video details retrieved: {}", video.toPrettyString());

        return video;
    }

    public List<SearchResult> getLiveStreamingChannels() throws GeneralSecurityException, IOException {
        logger.info("Fetching live streaming channels");

        YouTube youtubeService = getYouTubeService();

        YouTube.Search.List searchRequest = youtubeService.search()
                .list("snippet")
                .setKey(apiKey)
                .setEventType("live")
                .setType("video")
                .setOrder("date")
                .setMaxResults(20L);

        SearchListResponse searchResponse = searchRequest.execute();
        List<SearchResult> searchResults = searchResponse.getItems();

        if (searchResults == null || searchResults.isEmpty()) {
            logger.info("No live streaming channels found.");
            return List.of(); // Return an empty list if no live streams are found
        }

        logger.info("Found {} live streaming channels.", searchResults.size());
        return searchResults;
    }

    public List<LiveChatMessage> getChannelSuperChatMessages(String channelName) throws GeneralSecurityException, IOException {
        logger.info("Fetching super chat messages for channel: {}", channelName);

        Channel channel = getChannelInfoByChannelName(channelName);
        if (channel == null) {
            logger.error("Channel not found for name: {}", channelName);
            throw new IOException("Channel not found.");
        }

        String liveChatId = getLiveChatId(channel.getId());
        if (liveChatId == null) {
            logger.error("No live chat ID found for channel: {}", channelName);
            throw new IOException("No live chat found for the channel.");
        }

        YouTube youtubeService = getYouTubeService();

        YouTube.LiveChatMessages.List liveChatRequest = youtubeService.liveChatMessages()
                .list(liveChatId, "snippet,authorDetails")
                .setKey(apiKey);

        LiveChatMessageListResponse liveChatResponse = liveChatRequest.execute();
        List<LiveChatMessage> liveChatMessages = liveChatResponse.getItems();

        if (liveChatMessages == null || liveChatMessages.isEmpty()) {
            logger.info("No Super Chat messages found.");
            return List.of(); // Return an empty list if no Super Chat messages are found
        }

        logger.info("Found {} Super Chat messages.", liveChatMessages.size());
        return liveChatMessages;
    }

    private String getLiveChatId(String channelId) throws GeneralSecurityException, IOException {
        YouTube youtubeService = getYouTubeService();

        YouTube.Search.List searchRequest = youtubeService.search()
                .list("id,snippet")
                .setKey(apiKey)
                .setChannelId(channelId)
                .setEventType("live")
                .setType("video");

        SearchListResponse searchResponse = searchRequest.execute();
        List<SearchResult> searchResults = searchResponse.getItems();

        if (searchResults == null || searchResults.isEmpty()) {
            return null; // Return null if no live chat found
        }

        String liveBroadcastId = searchResults.get(0).getId().getVideoId();

        YouTube.Videos.List videoRequest = youtubeService.videos()
                .list("liveStreamingDetails")
                .setKey(apiKey)
                .setId(liveBroadcastId);

        VideoListResponse videoResponse = videoRequest.execute();
        List<Video> videos = videoResponse.getItems();

        if (videos == null || videos.isEmpty() || videos.get(0).getLiveStreamingDetails() == null) {
            return null; // Return null if no live streaming details found
        }

        return videos.get(0).getLiveStreamingDetails().getActiveLiveChatId();
    }
}
