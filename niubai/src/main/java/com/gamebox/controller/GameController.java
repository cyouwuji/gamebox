package com.gamebox.controller;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.gamebox.model.FacebookAppInformation;
import com.gamebox.model.LoginGameHistory;
import com.gamebox.model.Server;
import com.gamebox.model.DisplayType;
import com.gamebox.model.Users;
import com.gamebox.model.OpenStatusType;
import com.gamebox.service.FacebookAppInformationService;
import com.gamebox.service.GameService;
import com.gamebox.service.LoginGameHistoryService;
import com.gamebox.service.ServerService;
import com.gamebox.util.DateUtils;
import com.gamebox.util.HttpUtils;
import com.gamebox.util.WebUtils;

/**
 * Controller - 管理员
 * 
 * @author niuhongliang_tmp
 * @version 1.0
 */
@Controller
@RequestMapping("/games")
public class GameController {

    public static final String LOGIN_RESULT_SUCCESS = "success";

    public static final String LOGIN_PARAM_FAILURE = "param_failure";

    public static final String LOGIN_SERVER_FAILURE = "server_failure";

    public static final String LOGIN_URL_FAILURE = "url_failure";

    public static final String LOGIN_MAINTAIN = "maintain";

    @Autowired
    private GameService gameService;

    @Autowired
    private ServerService serverService;

    @Autowired
    private LoginGameHistoryService loginGameHistoryService;
    
    @Autowired
    private FacebookAppInformationService facebookAppInformationService;

    @RequestMapping(value = "/login")
    @ResponseBody
    public Map<String, String> login(Integer gameId, Integer serverId, HttpServletRequest request,
            HttpServletResponse response) {

        Users users = (Users) request.getSession().getAttribute(WebUtils.USER);
        if (users == null) {
            try {
                FacebookAppInformation facebookAppInformation = facebookAppInformationService.findByGameId(gameId);
                String appId = facebookAppInformation.getAppId();
                String appUrl = "https://apps.facebook.com/" + appId + "/";
                response.sendRedirect(appUrl);
                return null;
            }
            catch (IOException e) {
               e.printStackTrace();
               return null;
            } 
        }
        
        Map<String, String> resultMap = new HashMap<String, String>();
        Integer sid = null;
        Integer gid = gameId;
        if ("10000000".equals(serverId)) {
            sid = serverService.getNewestServerId(gid);
        }
        else {
            sid = serverId;
        }
        if (!serverService.serverExists(sid, gid, OpenStatusType.able, DisplayType.display)) {
            resultMap.put("result", LOGIN_SERVER_FAILURE);
            return resultMap;
        }
        Server stmp = serverService.findServerByGidAndSid(gid, sid);
        if (stmp.getStatus() != null && "maintain".equals(stmp.getStatus().name())) {
            resultMap.put("result", LOGIN_MAINTAIN);
            return resultMap;
        }
        Users user = (Users) request.getSession().getAttribute("user");
        String url = serverService.buildGameUrl(gid, sid, user);
        if (StringUtils.isBlank(url)) {
            resultMap.put("result", LOGIN_URL_FAILURE);
            return resultMap;
        }
        resultMap.put("result", LOGIN_RESULT_SUCCESS);
        if (request.getParameter("fb") != null) {
            url = url.replace("http:", "https:");
        }
        resultMap.put("url", url);
        resultMap.put("name", serverService.findServerByGidAndSid(gid, sid).getName());
        resultMap.put("timezone", serverService.findServerByGidAndSid(gid, sid).getTimezone().name());
        WebUtils.addCookie(request, response, "latestGame_" + gameId + "_" + user.getUserId(), serverId.toString(), WebUtils.COOKIE_AGE);
        // 向loginGameHistory表中插入数据,向userPlayInfo表中插入数据
        
        Date date = new Date();
        LoginGameHistory loginGameHistory = new LoginGameHistory();
        loginGameHistory.setUserId(user.getUserId());
        loginGameHistory.setUserIp(HttpUtils.getIp(request));
        loginGameHistory.setEmail("");
        loginGameHistory.setGameId(gid);
        loginGameHistory.setServerId(sid);
        loginGameHistory.setLoginTime(DateUtils.getCurrentTime());
        loginGameHistory.setCreateDate(date);
        loginGameHistory.setModifyDate(date);
        loginGameHistoryService.save(loginGameHistory);

        // 更新session中user的玩游戏的信息
        
        return resultMap;
    }

    @RequestMapping(value = "/playpage", method = RequestMethod.GET)
    public String playpage(Integer gameId, Integer serverId, ModelMap model) {

        
        if (serverId == null || serverId > 1000) { // 这就走最新服
            serverId = serverService.getNewestServerId(gameId);
        }
        model.addAttribute("serverId", serverId);
        
        FacebookAppInformation facebookAppInformation = facebookAppInformationService.findByGameId(gameId);
        
        String identifier = facebookAppInformation.getIdentifier();
        
        return identifier + "/play_" + identifier;
       
    }
}
