package cn.edu.sustech.cs209.chatting.server;

public class User {
    String username;
    LineStatus state=LineStatus.OFFLINE;
    public User(String username,LineStatus state){
        this.username=username;
        this.state=state;
    }
}
enum LineStatus{
    ONLINE,OFFLINE
};
