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
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoLiveStreamingDetails;

@Service
public class YouTubeService {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeService.class);

    @Value("${youtube.api.key}")
    private String apiKey;
    private static final String APPLICATION_NAME = "youtube subscriber";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public Channel getChannelInfoByChannelName(String channelName) throws GeneralSecurityException, IOException {
        logger.info("Fetching channel info for channel name: {}", channelName);

        String encodedChannelName = URLEncoder.encode(channelName, StandardCharsets.UTF_8);

        YouTube youtubeService = new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                null)
                .setApplicationName(APPLICATION_NAME)
                .build();

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

        YouTube youtubeService = new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                null)
                .setApplicationName(APPLICATION_NAME)
                .build();

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

}