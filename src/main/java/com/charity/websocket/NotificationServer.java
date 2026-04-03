package com.charity.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 活动通知 WebSocket 服务端
 */
@Slf4j
@Component
@ServerEndpoint("/api/ws/notification/{userId}")
public class NotificationServer {

    /**
     * 用来存放每个客户端对应的 NotificationServer 对象
     */
    private static final ConcurrentHashMap<String, NotificationServer> webSocketMap = new ConcurrentHashMap<>();

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    /**
     * 接收 userId
     */
    private String userId;

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        this.session = session;
        this.userId = userId;
        webSocketMap.put(userId, this);
        log.info("WebSocket 连接成功，用户ID：{}，当前在线人数：{}", userId, webSocketMap.size());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if (userId != null) {
            webSocketMap.remove(userId);
            log.info("WebSocket 连接关闭，用户ID：{}，当前在线人数：{}", userId, webSocketMap.size());
        }
    }

    /**
     * 收到客户端消息后调用的方法
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到来自客户端的消息: {}", message);
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 发生错误", error);
    }

    /**
     * 群发自定义消息
     */
    public static void sendInfo(String message) {
        log.info("推送消息到所有客户端: {}", message);
        for (NotificationServer item : webSocketMap.values()) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                log.error("WebSocket 推送消息失败", e);
            }
        }
    }

    /**
     * 发送自定义消息到指定客户端
     */
    public static void sendToUser(String userId, String message) {
        log.info("推送消息到客户端 {}: {}", userId, message);
        NotificationServer item = webSocketMap.get(userId);
        if (item != null) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                log.error("WebSocket 推送消息失败", e);
            }
        }
    }

    /**
     * 实现服务器主动推送
     */
    private void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }
}
