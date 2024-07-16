package com.youtube.controller;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.services.youtube.model.Channel;
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
    
}
