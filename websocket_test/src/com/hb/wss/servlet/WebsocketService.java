package com.hb.wss.servlet;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

/**
 * websocket服务端
 * Created by Hongbo on 2017/6/7.
 */
@ServerEndpoint("/websocket_test")
public class WebsocketService {

/*
    public WebsocketService() {
        tt();
    }
    public static void main (String[] args) {
        new WebsocketService();
        System.out.println("你好,Intellij IDEA!");
        gghhggggg
        http://wiki.jikexueyuan.com/project/intellij-idea-tutorial/eclipse-java-web-project-introduce.html

    }

    void tt () {        System.out.println("好用不？？？");System.out.println("好用不？？？");

    }*/
    @OnMessage
    public void onMessage(String message, Session session) throws IOException, InterruptedException {
        System.out.println("Received message is:" +  message);

        session.getBasicRemote().sendText("This is the first server message");

        int sentMessages = 0;
        while(sentMessages < 3){
            Thread.sleep(5000);
            session.getBasicRemote().
                    sendText("This is an intermediate server message. Count: "
                            + sentMessages);
            sentMessages++;
        }

        // Send a final message to the client
        session.getBasicRemote().sendText("This is the last server message");

    }
    @OnOpen
    public void onOpen() {
        System.out.println("Client connected");
    }

    @OnClose
    public void onClose() {
        System.out.println("Connection closed");
    }
}
