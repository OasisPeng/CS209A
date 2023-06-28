package cn.edu.sustech.cs209.chatting.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    static ConcurrentHashMap<String,User> connectedUsers=new ConcurrentHashMap<>();
    static ConcurrentHashMap<Integer, ArrayList<String>> groups=new ConcurrentHashMap<>();
    static ConcurrentHashMap<String,Socket> clients=new ConcurrentHashMap<>();
    static Integer onlineCnt=0;
    //the server will listen for incoming connections from clients and create a new thread or process for each new connection.
    public static void main(String[] args) throws IOException {
        System.out.println("Starting server");
        final int PORT=8888;
        ServerSocket server =new ServerSocket(PORT);

        System.out.println("Waiting for clients to connect...");
        while(true){
            try {
                Socket s = server.accept();
                System.out.println("Client connected.");
                // Create a new thread to handle the client's requests
                //The connectedUsers map is passed to each ChatHandler instance so that it can maintain a list of connected users.
                ChatHandler chatHandler=new ChatHandler(s,connectedUsers, clients,groups);
                Thread t=new Thread(chatHandler);
                t.start();
            }catch (Exception e){
                System.out.println("Exception:"+e);
            }
        }
    }
}
