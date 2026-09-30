package com.twocall.chat.dto.response;

import java.util.List;

public class TurnCredentialsResponse {
    private String username;
    private String password;
    private long ttl;
    private List<String> urls;

    public TurnCredentialsResponse() {}

    public TurnCredentialsResponse(String username, String password, long ttl, List<String> urls) {
        this.username = username;
        this.password = password;
        this.ttl = ttl;
        this.urls = urls;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }
}
