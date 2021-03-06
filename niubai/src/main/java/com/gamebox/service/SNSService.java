package com.gamebox.service;

import javax.servlet.http.HttpServletRequest;

import com.gamebox.model.Users;


public interface SNSService {

    Users getFacebookUser(String scopeId, String appId, String accessToken, HttpServletRequest request);

    Users getExistFacebookUser(String scopeId, String appId, String accessToken);
    
    Users saveFacebookUser(String scopeId, String appId, HttpServletRequest request);

    void deleteByUserId(Integer userId);
}
