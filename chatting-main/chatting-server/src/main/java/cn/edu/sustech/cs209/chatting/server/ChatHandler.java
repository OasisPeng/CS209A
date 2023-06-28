package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.server.User;
import javafx.scene.control.Alert;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/*
实现自定义协议
In the ChatHandler class, we can implement the logic for handling client requests, such as logging in, logging out, sending messages, and creating chat sessions. We can also implement methods for updating the status of users and broadcasting messages to the participants of a chat session.
 */
public class ChatHandler implements Runnable{
    private Socket s;
    private ConcurrentHashMap<String,User> connectedUsers;
    private ConcurrentHashMap<Integer, ArrayList<String>> groups;
    private ConcurrentHashMap<String,Socket> clients;
    private DataOutputStream out;
    private DataInputStream in;
    private String username;
    public ChatHandler(Socket s, ConcurrentHashMap<String, User> connectedUsers,ConcurrentHashMap<String,Socket> clients,ConcurrentHashMap<Integer, ArrayList<String>> groups){
        this.s=s;
        this.connectedUsers=connectedUsers;
        this.clients=clients;
        this.groups=groups;
    }
    public void sendMessageToClient(String message) throws IOException {
        out.writeUTF(message);
        out.flush();
    }

    public String receiveMessageFromClient() throws IOException {
        return in.readUTF();
    }
    @Override
    public void run() {
        try {
            out = new DataOutputStream(s.getOutputStream());
            in = new DataInputStream(s.getInputStream());
            while (true) {
                String command = receiveMessageFromClient();
                System.out.println("Request:" + command);
                executeCommand(command);
            }
        } catch (IOException e) {
            if(username!=null) {//??
                connectedUsers.get(username).state = LineStatus.OFFLINE;
            }
        }
    }

    public void executeCommand(String command) throws IOException {
        if(command.contains("create a user: ")){
            //判断用户名是否冲突
            String name=command.substring(15);
            username=name;
            for(String s :connectedUsers.keySet()){
                if (s.equals(name)){
                    sendMessageToClient("This username has already been used,please change the name");
                     return;
                }
            }
            User user=new User(name,LineStatus.ONLINE);
            connectedUsers.put(user.username, user);
            sendMessageToClient("Have created a user");
        }else if(command.contains("enter a user: ")){
            //FIXME:用户名输错的情况
            String enterUsername=command.substring(14);
            username=enterUsername;
            if(connectedUsers.containsKey(enterUsername)&& connectedUsers.get(enterUsername).state.equals(LineStatus.OFFLINE)){
                connectedUsers.get(enterUsername).state=LineStatus.ONLINE;
                sendMessageToClient("Have entered a user");
            }
        }else if(command.contains("get the user list by: ")){
            String name=command.substring(22);
            StringBuilder sb=new StringBuilder();
            for(String s:connectedUsers.keySet()){
                if (!s.equals(name)&&connectedUsers.get(s).state.equals(LineStatus.ONLINE)){
                    //用户名不能带空格,最好不要是数字
                    sb.append(s).append(" ");
                }
            }
            String str=sb.toString();
            if(str.length()!=0) {
                str = str.substring(0, str.length() - 1);
            }
            sendMessageToClient("list: "+str);
        }else if(command.contains("Get online count")){
            int count=0;
            for(String s:connectedUsers.keySet()){
                if (connectedUsers.get(s).state.equals(LineStatus.ONLINE)){
                    count++;
                }
            }
            sendMessageToClient("onlineCount: "+count);
        }else if(command.contains("Send a message from ")){
            //"Send a message from "+username+" to "+sendTo+" : "+text+" in "+ time
            int index=command.indexOf(" to ");
            String username=command.substring(20,index);
            int index1=command.indexOf(" : ");
            String sendTo=command.substring(index+4,index1);
            //int index2=command.indexOf(" in ");
            String s=command.substring(index1);
            if(connectedUsers.get(sendTo).state.equals(LineStatus.ONLINE)) {
                new DataOutputStream(clients.get(sendTo).getOutputStream()).writeUTF("Receive a message: " + username + s);
            }else {
                new DataOutputStream(clients.get(username).getOutputStream()).writeUTF("消息发送失败！对方已下线，无法接受消息");
            }
        }else if(command.contains("Send a group message from ")) {
            int index=command.indexOf(" to ");
            String username=command.substring(26,index);
            int index1=command.indexOf(" : ");
            int sendTo=Integer.parseInt(command.substring(index+4,index1));
            String s=command.substring(26);
            String sendToClient="Receive a group message: "+s;
            ArrayList<String> members=groups.get(sendTo);
            int count=0;
            for(String string:members){
                if (connectedUsers.get(string).state.equals(LineStatus.ONLINE)){
                    count++;
                }
            }
            if(count==1){
                new DataOutputStream(clients.get(username).getOutputStream()).writeUTF("消息发送失败！群友都下线了，还搁这发消息呢");
            }else {
                for (String str : members) {
                    if (!str.equals(username) && connectedUsers.get(str).state.equals(LineStatus.ONLINE)) {
                        new DataOutputStream(clients.get(str).getOutputStream()).writeUTF(sendToClient);
                    }
                }
            }
        }else if(command.contains("Send a file: ")){
            //服务器端可以将接收到的文件数据保存在磁盘上，供其他客户端下载
            //"Send a file: "+"from "+username+" to "+sendTo+"filename: "+fileName+"type: "+fileType+"content: "+ Arrays.toString(fileData)+" in "+ time;
            int index0=command.indexOf("from ");
            int index1=command.indexOf(" to ");
            int index2=command.indexOf("filename: ");
            String from=command.substring(index0+5,index1);
            String to=command.substring(index1+4,index2);
            String s=command.substring(index2);
            if(connectedUsers.get(to).state.equals(LineStatus.ONLINE)) {
                new DataOutputStream(clients.get(to).getOutputStream()).writeUTF("Receive a file: "+from+s);
            }else {
                new DataOutputStream(clients.get(username).getOutputStream()).writeUTF("文件发送失败！对方已下线，无法接受文件");
            }
        }//else if(){

        //}
        else if(command.contains("Client name: ")){
            String name=command.substring(13);
            clients.put(name,s);
            //clients.entrySet().stream().forEach(System.out::println);
        }else if (command.contains("Create a Group: ")){
            String names=command.substring(16);
            String[] strings=names.split(" ");
            ArrayList<String> group= Arrays.stream(strings).collect(Collectors.toCollection(ArrayList::new));
            int id=groups.size();
            groups.put(id, group);
            StringBuilder sb=new StringBuilder();
            String title="";
            Collections.sort(group);
            if (group.size()<=3){
                for(int i=0;i<group.size();i++){
                    if(i!=group.size()-1) {
                        sb.append(group.get(i)).append(",");
                    }else {
                        sb.append(group.get(i));
                    }
                }
            }else {
                sb.append(group.get(0)).append(",");
                sb.append(group.get(1)).append(",");
                sb.append(group.get(2)).append("...");
                sb.append(" (").append(group.size()).append(")");
            }
            title=sb.toString();
            for (String member:group){
                    //ID,name,creator
                    new DataOutputStream(clients.get(member).getOutputStream()).writeUTF("Group ID: " + id+" Creator :"+username+" Name: "+title+" Members: "+ group);
            }
        }
    }
}
