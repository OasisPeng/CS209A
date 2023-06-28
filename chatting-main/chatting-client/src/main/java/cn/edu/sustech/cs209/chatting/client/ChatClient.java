package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatClient implements Runnable {
    private Socket socket;//客户端的socket
    public DataOutputStream out;
    public DataInputStream in;
    public String username;
    public Map<String, List<Message>> messages=new TreeMap<>();//找到该用户和每个用户对应的聊天记录,包括该用户发出去的和接收到的
    public Map<String,Integer> unread=new HashMap<>();//该用户是否有未读消息
    public Map<String,Boolean> isOnline=new HashMap<>();

    public Map<Integer,List<Message>> groupMessages =new HashMap<>();//该用户所在的群组的聊天记录
    public Map<Integer,Integer> unreadGroup=new HashMap<>();
    public Map<Integer,String> groupNames=new HashMap<>();
    public Map<Integer,ArrayList<String>> groupMember=new HashMap<>();

    public volatile boolean nameConflict=false;
    public volatile int onlineCount;
    public volatile String[] s;//除自己以外的所有在线用户
    public volatile int unreadMessage;
    public volatile int createOneGroup=-1;
    public volatile int newGroupNumber;
    //检测是否连接服务器
    public volatile boolean connected=true;

    public volatile boolean fileFailed=false;
    public volatile boolean messageFailed=false;
    public volatile boolean groupMessageFailed=false;
    //***改进：客户端和服务端之间可以用封装类沟通，不一定是字符串
    public ChatClient(String serverAddress, int serverPort) throws  IOException {
        socket = new Socket(serverAddress, serverPort);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
    }

    public void sendMessage(String message) throws IOException {
        out.writeUTF(message);
        out.flush();
    }

    public String receiveMessage() throws IOException {
        return in.readUTF();
    }

    public void close() throws IOException {
        socket.close();
    }
//接收服务端转接的消息，放入map中
    @Override
    public void run() {
        try {
            while(true){
                    String back = receiveMessage();
                    System.out.println("Receive: " + back);
                    executeCommand(back);

            }
        }catch (IOException e){
            System.out.println("服务器已关闭");
            connected=false;
        }
    }
    //response.equals("This username has already been used,please change the name")
    public void executeCommand(String back) throws IOException {
        if(back.contains("Receive a message: ")){
            //username+" : "+text+" in "+ time
            String s=back.substring(19);
            int index0=s.indexOf(" : ");
            int index1=s.indexOf(" in ");
            String uname=s.substring(0,index0);
            String text=s.substring(index0+3,index1);
            String time=s.substring(index1+4);
            LocalDateTime t=LocalDateTime.parse(time.substring(0,19).replace('T',' '), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            Message mes=new Message(t,uname,username,text);
            if(!messages.containsKey(uname)){
                messages.put(uname,new LinkedList<>());
                unread.put(uname,0);
            }
            messages.get(uname).add(mes);
            unread.put(uname,unread.get(uname)+1);
            isOnline.put(uname,true);
           // System.out.println(isOnline);
            unreadMessage++;
        }else if(back.contains("Receive a group message: ")) {
            //username+" to "+sendTo+" : "+text+" in "+ time
            String s=back.substring(25);
            int index0=s.indexOf(" to ");
            int index1=s.indexOf(" : ");
            int index2=s.indexOf(" in ");
            String from=s.substring(0,index0);
            int id=Integer.parseInt(s.substring(index0+4,index1));
            String text=s.substring(index1+3,index2);
            String time=s.substring(index2+4);
            LocalDateTime t=LocalDateTime.parse(time.substring(0,19).replace('T',' '), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            Message mes=new Message(t,from,id+"",text);
            groupMessages.get(id).add(mes);
            unreadGroup.put(id,unreadGroup.get(id)+1);
            unreadMessage++;
        }else if(back.contains("Receive a file: ")){
            //System.out.println(111);
            //from+"filename: "+fileName+"type: "+fileType+"content: "+ Arrays.toString(fileData)+" in "+ time;
            String s=back.substring(16);
            int index0=s.indexOf("filename: ");
            int index1=s.indexOf("type: ");
            int index2=s.indexOf(" in ");
            int index3=s.indexOf("type: ");
            int index4=s.indexOf("content: ");
            String uname=s.substring(0,index0);
            //System.out.println(uname);
            String filename=s.substring(index0+10,index1);
            String time=s.substring(index2+4);
            LocalDateTime t=LocalDateTime.parse(time.substring(0,19).replace('T',' '), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            //接收方的Message中保存文件信息
            String type=s.substring(index3+6,index4);
            String content=s.substring(index4+9,index2);
            String[] byteStringArray = content.substring(1, content.length() - 1).split(", ");
            byte[] newArray = new byte[byteStringArray.length];
            for (int i = 0; i < byteStringArray.length; i++) {
                newArray[i] = Byte.parseByte(byteStringArray[i]);
            }
            Message mes=new Message(t,uname,username,filename,type,newArray,true);
            if(!messages.containsKey(uname)){
                messages.put(uname,new LinkedList<>());
                unread.put(uname,0);
            }
            messages.get(uname).add(mes);
            unread.put(uname,unread.get(uname)+1);
            isOnline.put(uname,true);
            unreadMessage++;
        }else if(back.equals("This username has already been used,please change the name")){
            nameConflict=true;
        }else if(back.equals("Have created a user")){
            nameConflict=false;
        }else if(back.contains("onlineCount: ")){
            String s=back.substring(13);
            onlineCount=Integer.parseInt(s);
        }else if(back.contains("list: ")){
            String str=back.substring(6);
            s=str.split(" ");
        }else if(back.contains("Group ID: ")){
            int index0=back.indexOf(" Creator :");
            int index1=back.indexOf(" Name: ");
            int index2=back.indexOf(" Members: ");
            int id=Integer.parseInt(back.substring(10,index0));
            String creator=back.substring(index0+10,index1);
            String groupTitle=back.substring(index1+7,index2);
            String l=back.substring(index2+10);
            String[] stringArray = l.substring(1, l.length() - 1).split(", ");
            ArrayList<String> newList = new ArrayList<>(Arrays.asList(stringArray));
            if(creator.equals(username)){
                createOneGroup=id;
            }else {
                newGroupNumber++;
            }
            groupNames.put(id,groupTitle);
            groupMessages.put(id,new LinkedList<>());
            unreadGroup.put(id,0);
            System.out.println(newList);
            groupMember.put(id,newList);
        }else if(back.equals("消息发送失败！对方已下线，无法接受消息")){
            messageFailed=true;
        }else if(back.equals("消息发送失败！群友都下线了，还搁这发消息呢")){
            groupMessageFailed=true;
        }else if(back.equals("文件发送失败！对方已下线，无法接受文件")) {
            fileFailed=true;
        }else if(back.equals("Have entered a user")){
            try {
                FileInputStream fis = new FileInputStream("chatMessages.ser");
                ObjectInputStream ois = new ObjectInputStream(fis);
                messages = (Map<String, List<Message>>) ois.readObject();
                ois.close();
                fis.close();
                FileInputStream fis1 = new FileInputStream("groupMessages.ser");
                ObjectInputStream ois1 = new ObjectInputStream(fis1);
                groupMessages = (Map<Integer, List<Message>>) ois1.readObject();
                ois1.close();
                fis1.close();
                FileInputStream fis2 = new FileInputStream("groupNames.ser");
                ObjectInputStream ois2 = new ObjectInputStream(fis2);
                groupNames = (Map<Integer, String>) ois2.readObject();
                ois2.close();
                fis2.close();
                FileInputStream fis3 = new FileInputStream("groupMember.ser");
                ObjectInputStream ois3 = new ObjectInputStream(fis3);
                groupMember = (Map<Integer, ArrayList<String>>) ois3.readObject();
                ois3.close();
                fis3.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

