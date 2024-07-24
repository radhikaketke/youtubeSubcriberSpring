package com.youtube.controller;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.LiveChatMessage;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.youtube.service.YouTubeService;

@RestController
@RequestMapping("/api/youtube")
public class YouTubeController {

    @Autowired
    private YouTubeService youTubeService;

    @GetMapping("/channel/{channelName}")
    public ResponseEntity<Channel> getChannelInfo(@PathVariable String channelName) throws GeneralSecurityException, IOException {
        Channel channel = youTubeService.getChannelInfoByChannelName(channelName);
        return ResponseEntity.ok(channel);
    }
    
    @GetMapping("/live/{channelName}")
    public BigInteger getLiveViewersCount(@PathVariable String channelName) throws GeneralSecurityException, IOException {
        return youTubeService.getLiveViewersCountByChannelName(channelName);
    }
    
    @GetMapping("/video/{videoId}")
    public ResponseEntity<Video> getVideoDetails(@PathVariable String videoId) throws GeneralSecurityException, IOException {
        Video video = youTubeService.getVideoDetails(videoId);
        return ResponseEntity.ok(video);
    }
    
    @GetMapping("/live-channels")
    public ResponseEntity<List<SearchResult>> getLiveStreamingChannels() throws GeneralSecurityException, IOException {
        List<SearchResult> liveChannels = youTubeService.getLiveStreamingChannels();
        return ResponseEntity.ok(liveChannels);
    }
    
    @GetMapping("/superchat/{channelName}")
    public ResponseEntity<List<LiveChatMessage>> getSuperChatMessages(@PathVariable String channelName) {
        try {
            List<LiveChatMessage> superChatMessages = youTubeService.getChannelSuperChatMessages(channelName);
            return ResponseEntity.ok(superChatMessages);
        } catch (GeneralSecurityException | IOException e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }
    
}
