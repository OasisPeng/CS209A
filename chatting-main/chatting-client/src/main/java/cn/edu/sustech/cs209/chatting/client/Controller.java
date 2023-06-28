package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.AxisBuilder;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Controller implements Initializable {
  private static ChatClient chatClient; //找到该用户和每个用户对应的聊天记录,包括该用户发出去的和接收到的
  @FXML
  ListView<Message> chatContentList;
  @FXML
  ListView<ChatItem> chatList;
  @FXML
  Label currentUsername;
  @FXML
  Label currentOnlineCnt;
  @FXML
  TextArea inputArea;
  @FXML
  Button emoji;
  @FXML
  Circle isConnectedToServer;
  //左侧聊天项
  ObservableList<ChatItem> chatItems = FXCollections.observableArrayList();

  private class ChatItem{
    String name;
    int groupID;
    boolean isPrivate;

    public ChatItem(String name){
      this.name = name;
      isPrivate = true;
    }

    public ChatItem(int groupID){
      this.groupID = groupID;
      isPrivate = false;
    }
  }

  String username;
  AtomicReference<ChatItem> user = new AtomicReference<>(); //有什么用？？序列化

  public static ChatClient getClient(){
    return chatClient;
  }
  //TODO: 增加按键更新在线人数
  //TODO: 增加一个退出按键，退出同时关闭socket和thread,会显示已下线
  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    // 创建登录或注册对话框
    Dialog<ButtonType> dialo = new Dialog<>();
    dialo.setTitle("选择用户类型");
    dialo.setHeaderText("请选择您是要登录还是注册：");
    // 添加登录和注册按钮
    ButtonType loginButtonType = new ButtonType("登录");
    ButtonType registerButtonType = new ButtonType("注册");
    dialo.getDialogPane().getButtonTypes().addAll(loginButtonType, registerButtonType);

    // 显示对话框并等待用户选择
    dialo.showAndWait().ifPresent(buttonType -> {
      if (buttonType == loginButtonType) {
        // 用户选择登录，打开登录窗口
        try {
          chatClient = new ChatClient("localhost",8888);
        } catch (IOException e) {
          Alert alert = new Alert(Alert.AlertType.INFORMATION);
          alert.setTitle("提示信息");
          alert.setHeaderText(null);
          alert.setContentText("未连接服务器");
          alert.showAndWait();
          Platform.exit();
        }
        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("登录");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");
        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
          try {
            //开一个client线程
            chatClient = new ChatClient("localhost",8888);
            Thread thread = new Thread(chatClient);
            thread.start();
            String name = input.get();
            String command = "enter a user: " + name;
            System.out.println("Sending: " + command);
            chatClient.sendMessage(command);
            Thread.sleep(100);
            username = name;
            chatClient.username = username;
            currentUsername.setText("Current User: " + username);
            chatClient.sendMessage("Client name: " + username);
            System.out.println(chatClient.messages);
            System.out.println(chatClient.groupMessages);
            for (String str: chatClient.messages.keySet()) {
              chatClient.unread.put(str, 0);
              chatClient.isOnline.put(str, true);
              chatItems.add(new ChatItem(str));
              chatContentList.setItems(FXCollections.observableList(chatClient.messages.get(str)));
            }
            for (int t : chatClient.groupMessages.keySet()) {
              chatClient.unreadGroup.put(t,0);
              chatItems.add(new ChatItem(t));
              chatContentList.setItems(FXCollections.observableList(chatClient.groupMessages.get(t)));
            }
            chatContentList.setCellFactory(new MessageCellFactory());
            chatList.setItems(chatItems);
            chatList.setCellFactory(new ItemCellFactory());
          } catch (IOException | InterruptedException e) {
            e.printStackTrace();
          }
        } else {
          System.out.println("Invalid username " + input + ", exiting");
          Platform.exit();
        }
      } else if (buttonType == registerButtonType) {
        // 用户选择注册，打开注册窗口
        try {
           chatClient = new ChatClient("localhost",8888);
        } catch (IOException e) {
          Alert alert = new Alert(Alert.AlertType.INFORMATION);
          alert.setTitle("提示信息");
          alert.setHeaderText(null);
          alert.setContentText("未连接服务器");
          alert.showAndWait();
          Platform.exit();
        }
        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("注册");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");
        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
        /*
          Check if there is a user with the same name among the currently logged-in users,
                 if so, ask the user to change the username
         */
                    //创建一个client和一个user,要判断用户名是否冲突
          try {
            Thread thread = new Thread(chatClient);
            thread.start();
            String name = input.get();
            String command = "create a user: " + name;
            System.out.println("Sending: " + command);
            chatClient.sendMessage(command);
            Thread.sleep(100);
            // System.out.println(chatClient.nameConflict);
            while (chatClient.nameConflict) {
              Optional<String> input1 = dialog.showAndWait();
              if (input1.isPresent() && !input1.get().isEmpty()) {
                name = input1.get();
                command = "create a user: " + name;
                System.out.println("Sending: " + command);
                chatClient.sendMessage(command);
              }
              Thread.sleep(10);
              if (!chatClient.nameConflict){
                break;
              }
            }
            username = name;
            chatClient.username = username;
            currentUsername.setText("Current User: " + username);
            chatClient.sendMessage("Client name: " + username);
          } catch (InterruptedException | IOException e) {
            e.printStackTrace();
          }
        } else {
          System.out.println("Invalid username " + input + ", exiting");
          Platform.exit();
        }
      }
    });

    chatContentList.setCellFactory(new MessageCellFactory());
    chatList.setItems(chatItems);
    //点击左侧聊天项就跳转到对应窗口，打开红点消失，显示聊天内容
    chatList.setOnMouseClicked(event -> {
      if (event.getClickCount() == 1) { // 判断是否是单击事件
        ChatItem selectedItem = chatList.getSelectionModel().getSelectedItem();
        int index = chatItems.indexOf(selectedItem);
        if (index >= 0) {
          user.set(chatItems.get(index));
          //  System.out.println("所选聊天项："+user.get());
          if (user.get().isPrivate && chatClient.unread.get(user.get().name) > 0) {
            chatClient.unread.put(user.get().name, 0);
          }
          if (!user.get().isPrivate && chatClient.unreadGroup.get(user.get().groupID) > 0) {
            chatClient.unreadGroup.put(user.get().groupID, 0);
          }
          chatList.setCellFactory(new ItemCellFactory());
          //chatList.scrollTo(index);
          if (!user.get().isPrivate) {
            chatContentList.setItems(FXCollections.observableList(chatClient.groupMessages.get(user.get().groupID)));
          } else {
            chatContentList.setItems(FXCollections.observableList(chatClient.messages.get(user.get().name)));
          }
          chatContentList.setCellFactory(new MessageCellFactory());
        }
      }
    });
    //消息发送失败
    Thread failedThread = new Thread(() -> {
    //FIXME:发送失败的消息应该不再显示在聊天窗口里
      while (true) {
        if (chatClient.messageFailed || chatClient.fileFailed || chatClient.groupMessageFailed) {
            Platform.runLater(() -> {
              if (chatClient.fileFailed) {
                chatClient.fileFailed = false;
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "文件发送失败！对方已下线，无法接受文件");
                alert.showAndWait();
              } else if (chatClient.groupMessageFailed) {
                chatClient.groupMessageFailed = false;
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "消息发送失败！群友都下线了，还搁这发消息呢");
                alert.showAndWait();
                chatClient.groupMessageFailed = false;
              } else if (chatClient.messageFailed) {
                chatClient.messageFailed = false;
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "消息发送失败！对方已下线，无法接受消息");
                alert.showAndWait();
              }
            });
        }
      }
    });
    failedThread.start();
    //左侧显示新被拉入的群聊 显示服务器状态
    Thread newGroupThread = new Thread(() -> {
      while (true){
        //
        if (chatClient.connected) {
          isConnectedToServer.setFill(Color.GREEN);
        } else {
          isConnectedToServer.setFill(Color.RED);
        }
        //
        synchronized (this) {
          if (chatClient.newGroupNumber > 0) {
            Platform.runLater(() -> {
              for (int id : chatClient.groupMessages.keySet()) {
                boolean contain = false;
                for (ChatItem c : chatItems) {
                  if (!c.isPrivate && c.groupID == id) {
                    contain = true;
                  }
                }
                if (!contain) {
                  chatItems.add(new ChatItem(id));
                  chatList.setItems(chatItems);
                }
              }
              chatList.setCellFactory(new ItemCellFactory());
            });
            chatClient.newGroupNumber = 0;
          }
        }
      }
    });
    newGroupThread.start();
    //更新当前窗口聊天内容,未读消息显示红点
    Thread chatContentThread = new Thread(() -> {
      while (true) {
        synchronized (this) {
          if (chatClient.unreadMessage > 0) {
            Platform.runLater(() -> {
              for (String s : chatClient.messages.keySet()) {
                if (chatClient.unread.get(s) > 0) {
                  boolean contain = false;
                  for (ChatItem c : chatItems) {
                    if (c.isPrivate && c.name.equals(s)) {
                      contain = true;
                    }
                  }
                  if (!contain) {
                    chatItems.add(new ChatItem(s));
                    chatList.setItems(chatItems);
                  }
                  if (user.get() != null&&user.get().name!=null && user.get().name.equals(s) && user.get().isPrivate) {//?
                          chatClient.unread.put(s, 0);
                          chatContentList.setItems(FXCollections.observableList(chatClient.messages.get(s)));
                          chatContentList.setCellFactory(new MessageCellFactory());
                  }
                  chatList.setCellFactory(new ItemCellFactory());
                }
              }
              for (int t : chatClient.groupMessages.keySet()) {
                if (chatClient.unreadGroup.get(t) > 0) {
                  if (user.get() != null && user.get().groupID == t && !user.get().isPrivate) {
                    chatClient.unreadGroup.put(t, 0);
                    chatContentList.setItems(FXCollections.observableList(chatClient.groupMessages.get(t)));
                    chatContentList.setCellFactory(new MessageCellFactory());
                  }
                  chatList.setCellFactory(new ItemCellFactory());
                }
              }
            });
            chatClient.unreadMessage = 0;
          }
        }
      }
    });
    chatContentThread.start();
  }
  @FXML
  public void updateOnlineList() throws IOException, InterruptedException {
    //更新私聊用户在线情况
    chatClient.sendMessage("get the user list by: " + username);
    Thread.sleep(20);
    for (String str : chatClient.isOnline.keySet()) {
      if ( !Arrays.asList(chatClient.s).contains(str)) {
        chatClient.isOnline.put(str, false);
      }
      // System.out.println(chatClient.isOnline);
      chatList.setItems(chatItems);
      chatList.setCellFactory(new ItemCellFactory());
    }
    //消息按时间排序
    chatItems.sort((o1, o2) -> {
      LocalDateTime date1 = LocalDateTime.MIN;
      LocalDateTime date2 = LocalDateTime.MIN;
      if (o1.isPrivate) {
        List<Message> m = chatClient.messages.get(o1.name);
        if (m.size() > 0&&m.get(m.size() - 1).getTimestamp() != null) {//?
          date1 = m.get(m.size() - 1).getTimestamp();
        }
      } else {
        List<Message> m = chatClient.groupMessages.get(o1.groupID);
        if (m.size() > 0&&m.get(m.size() - 1).getTimestamp() != null) {
          date1 = m.get(m.size() - 1).getTimestamp();
        }
      }
      if (o2.isPrivate) {
        List<Message> m = chatClient.messages.get(o2.name);
        if (m.size() > 0 && m.get(m.size() - 1).getTimestamp() != null) {
          date2 = m.get(m.size() - 1).getTimestamp();
        }
      } else {
        List<Message> m = chatClient.groupMessages.get(o2.groupID);
        if (m.size() > 0 && m.get(m.size() - 1).getTimestamp() != null) {
          date2 = m.get(m.size() - 1).getTimestamp();
        }
      }
      return date2.compareTo(date1);
    });
    chatList.setItems(chatItems);
    chatList.setCellFactory(new ItemCellFactory());
    }
  @FXML
  public void insertSmileEmoji() {
        double BUTTON_SIZE=5000.0;
        List<String> emojis = Arrays.asList("\uD83D\uDE00","\uD83D\uDE04","\uD83D\uDE1C","\uD83D\uDE1D","\uD83D\uDE01", "\uD83D\uDE02", "\uD83D\uDE03" ,"\uD83C\uDF89", "\uD83C\uDF1E",
                "\uD83C\uDF55", "\uD83D\uDC36", "\uD83D\uDC31", "\uD83D\uDE80", "\uD83C\uDFB8", "\uD83D\uDCF7","\uD83C\uDFE0",
                "\uD83C\uDF08", "\uD83D\uDEB2", "\uD83C\uDF54","\uD83C\uDF7A","\uD83C\uDFAD",
                "\uD83C\uDF39", "\uD83C\uDF0A", "\uD83D\uDCD6", "\uD83D\uDCA1");
        Stage emojiStage = new Stage();
        emojiStage.initModality(Modality.APPLICATION_MODAL);
        emojiStage.setTitle("Emoji");
        // 计算每行表情数量
        int emojisPerRow = 5;
        // 创建一个VBox，用于显示所有表情
        VBox emojiBox = new VBox();
        emojiBox.setSpacing(5);
        emojiBox.setAlignment(Pos.CENTER);

        // 添加所有表情到HBox中
        HBox emojiRow = new HBox();
        for (int i = 0; i < emojis.size(); i++) {
            String emoji = emojis.get(i);
            Button emojiButton = new Button(emoji);
            emojiButton.setPrefSize(BUTTON_SIZE,BUTTON_SIZE);
            emojiButton.setOnAction(event -> {
                inputArea.appendText(emoji);
            });
            emojiRow.getChildren().add(emojiButton);

            // 当一行的表情数量达到预期时，添加HBox到VBox中并创建新的HBox
            if (i % emojisPerRow == emojisPerRow - 1 || i == emojis.size() - 1) {
                emojiBox.getChildren().add(emojiRow);
                emojiRow = new HBox();
            }
        }
        // 将VBox和“确定”按钮添加到一个StackPane中
        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(emojiBox);
        // 创建一个新的Scene，并将StackPane设置为根节点
        Scene emojiScene = new Scene(stackPane, 150, 150);
        // 将Scene设置为Stage的Scene，并显示Stage
        emojiStage.setScene(emojiScene);
        emojiStage.show();
    }

  @FXML
  public void chooseFile() throws IOException {
        // Create a new FileChooser
        FileChooser fileChooser = new FileChooser();
        // Set the title of the dialog
        fileChooser.setTitle("Choose a file");
        // Show the dialog and wait for the user to choose a file
        File file = fileChooser.showOpenDialog(null);
        // If a file was chosen, send it to the server
        if (file != null) {
            //显示文件
            byte[] fileData = Files.readAllBytes(file.toPath());
            String fileName = file.getName();
            String fileType = Files.probeContentType(file.toPath());
            if(user.get().isPrivate) {
                LocalDateTime time = LocalDateTime.now();
                String sendTo = user.get().name;
                Message mess = new Message(time, username, sendTo, file,true);
                chatClient.messages.get(sendTo).add(mess);
                chatContentList.setItems(FXCollections.observableList(chatClient.messages.get(sendTo)));
                chatContentList.setCellFactory(new MessageCellFactory());
                String command="Send a file: "+"from "+username+" to "+sendTo+"filename: "+fileName+"type: "+fileType+"content: "+ Arrays.toString(fileData)+" in "+ time;
                System.out.println("Request: "+command);
                chatClient.sendMessage(command);
            }else {
                LocalDateTime time = LocalDateTime.now();
                String sendTo = user.get().groupID+"";
                Message mess = new Message(time, username, sendTo, file,true);
                chatClient.groupMessages.get(Integer.parseInt(sendTo)).add(mess);
                chatContentList.setItems(FXCollections.observableList(chatClient.groupMessages.get(Integer.parseInt(sendTo))));
                chatContentList.setCellFactory(new MessageCellFactory());
                //TODO:群组中发送文件
            }
        }
    }

  @FXML
  public void createPrivateChat() throws IOException, InterruptedException {
        String command="Get online count";
        System.out.println("Request: "+command);
        chatClient.sendMessage(command);

        Thread.sleep(10);
        currentOnlineCnt.setText("Online: " + chatClient.onlineCount);

        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();

        //  get the user list from server, the current user's name should be filtered out
        command="get the user list by: "+username;
        chatClient.sendMessage(command);

        Thread.sleep(10);
        userSel.getItems().addAll(chatClient.s);

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            String selectedUser=userSel.getSelectionModel().getSelectedItem();
            chatClient.isOnline.put(selectedUser,true);
            boolean contain=false;
            for(ChatItem c:chatItems) {
                if (c.isPrivate && c.name.equals(selectedUser)) {
                    user.set(c);
                    System.out.println("所选聊天项："+user.get());
                    contain=true;
                }
            }
            if (!contain) {
                ChatItem ci=new ChatItem(selectedUser);
                chatItems.add(ci);
                user.set(ci);
                List<Message> l=new LinkedList<>();
                chatClient.messages.put(selectedUser, l);
                chatClient.unread.put(selectedUser,0);
                // add a new chat item with a red dot
                chatList.setCellFactory(new ItemCellFactory());

            }
            // if the current user already chatted with the selected user, just open the chat with that user
            // otherwise, create a new chat item in the left panel, the title should be the selected user's name
            chatContentList.setItems(FXCollections.observableList(chatClient.messages.get(selectedUser)));
            chatContentList.setCellFactory(new MessageCellFactory());
            stage.close();

        });

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();
        //TODO: 对方也应该自动增加与用户的聊天项，旁边会有一个红点表示新创建的聊天或者未读消息
    }

  /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
  @FXML
  public void createGroupChat() throws IOException, InterruptedException {
    String command = "Get online count";
    chatClient.sendMessage(command);
    Thread.sleep(10);
    currentOnlineCnt.setText("Online: " + chatClient.onlineCount);

    Stage stage = new Stage();
    ListView<String> userListView = new ListView<>();
    command = "get the user list by: " + username;
    chatClient.sendMessage(command);
    Thread.sleep(10);
    userListView.getItems().addAll(chatClient.s);
    // 为ListView中的每个用户创建一个CheckBox
    ArrayList<String> members=new ArrayList<>();
    members.add(username);
    ObservableList<Node> userList = userListView.getItems().stream()
                .map(uSer -> {
                    CheckBox checkBox = new CheckBox(uSer);
                    checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue) {
                            members.add(uSer);
                        } else {
                            members.remove(uSer);
                        }
                    });
                    return checkBox;
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        // 将创建的CheckBox添加到ListView中
        userListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String user, boolean empty) {
                super.updateItem(user, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    //setText(user);
                    int index = getIndex();
                    setGraphic(userList.get(index));
                }
            }
        });
        Button okButton = new Button("OK");
        okButton.setOnAction(e -> {
            //将用户列表传给服务端

            StringBuilder s=new StringBuilder();
            for(String str:members){
                s.append(str).append(" ");
            }
            s.deleteCharAt(s.length()-1);
            String names=s.toString();
            try {
                chatClient.sendMessage("Create a Group: "+names);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            try {
                Thread.sleep(1000);
                if (chatClient.createOneGroup!=-1){
                    //chatClient.groupMember.put(chatClient.createOneGroup,members);
                    ChatItem ci=new ChatItem(chatClient.createOneGroup);
                    chatItems.add(ci);
                    user.set(ci);
                    chatList.setCellFactory(new ItemCellFactory());
                    chatContentList.setItems(FXCollections.observableList(chatClient.groupMessages.get(chatClient.createOneGroup)));
                    chatContentList.setCellFactory(new MessageCellFactory());
                    chatClient.createOneGroup=-1;
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            stage.close();
                });
        // 在场景图中添加ListView
        VBox vbox = new VBox();
        vbox.getChildren().addAll(userListView,okButton);
        Scene scene = new Scene(vbox);
        stage.setScene(scene);
        stage.show();



    }
    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    /*
    1.在客户端，可以为发送消息的TextArea组件添加一个“发送”按钮。当用户输入完消息后，点击按钮将消息文本和接收方用户名发送到服务端。
    2.在服务端收到客户端发送的消息后，从该列表中查找接收方用户，并将消息发送给该用户
    3.在客户端，可以通过监听服务端发送的消息实现接收消息的功能。可以使用JavaFX的Socket和InputStream实现监听服务端的消息，并使用Platform.runLater方法在JavaFX主线程中更新UI组件。
     */
    @FXML
    public void doSendMessage() throws IOException {
        //接发消息需要注意并发问题
        //Message存在messages中，显示在charContentList上
        if (!inputArea.getText().equals("")) {
            //判断是私聊还是群聊
            if(user.get().isPrivate) {
                String text = inputArea.getText();
                LocalDateTime time = LocalDateTime.now();
                String sendTo = user.get().name;
                Message mess = new Message(time, username, sendTo, text);
                chatClient.messages.get(sendTo).add(mess);
                chatContentList.setItems(FXCollections.observableList(chatClient.messages.get(sendTo)));
                chatContentList.setCellFactory(new MessageCellFactory());
                inputArea.clear();

                String command = "Send a message from " + username + " to " + sendTo + " : " + text + " in " + time;
                System.out.println("Request: " + command);
                chatClient.sendMessage(command);
            }else {
                String text = inputArea.getText();
                LocalDateTime time = LocalDateTime.now();
                String sendTo = user.get().groupID+"";
                Message mess = new Message(time, username, sendTo, text);
                chatClient.groupMessages.get(Integer.parseInt(sendTo)).add(mess);
                chatContentList.setItems(FXCollections.observableList(chatClient.groupMessages.get(Integer.parseInt(sendTo))));
                chatContentList.setCellFactory(new MessageCellFactory());
                inputArea.clear();

                String command = "Send a group message from " + username + " to " + sendTo + " : " + text + " in " + time;
                System.out.println("Request: " + command);
                chatClient.sendMessage(command);
            }
        }else {
            //TODO:提示消息不能为空
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "消息不能为空！");
            alert.showAndWait();
        }

    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class ItemCellFactory implements Callback<ListView<ChatItem>,ListCell<ChatItem>>{

        @Override
        public ListCell<ChatItem> call(ListView<ChatItem> param) {
            return new ListCell<ChatItem>(){
                @Override
                public void updateItem(ChatItem item, boolean empty) {
                    //群聊后面有一个按键，点击可查看在线用户列表
                    //私聊后面有一个红/绿点，用来显示该用户是否在线
                    super.updateItem(item, empty);
                    if (empty || Objects.isNull(item)) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    HBox hbox = new HBox();
                    if(item.isPrivate) {
                        Label label = new Label(item.name);
                        {
                            hbox.getChildren().addAll(label);
                            if(chatClient.unread.containsKey(item.name)) {//这样可以避免一个奇怪的bug???
                                if (chatClient.unread.get(item.name) > 0) {
                                    Circle dot = new Circle(3);
                                    dot.setFill(Color.RED);
                                    hbox.getChildren().add(dot);
                                }
                            }
                            Circle circle = new Circle(5,Color.GREEN);
                            if (chatClient.isOnline.containsKey(item.name)) {
                                if(!chatClient.isOnline.get(item.name)) {
                                    circle.setFill(Color.RED);
                                }
                            }
                            hbox.getChildren().add(circle);
                            HBox.setHgrow(circle, Priority.ALWAYS); // 设置Circle在HBox中的水平拉伸方式
                            HBox.setMargin(circle, new Insets(0, 0, 0, 170));

                            hbox.setSpacing(10);
                            hbox.setAlignment(Pos.CENTER_LEFT);
                        }

                        setText(null);
                        setGraphic(null);
                        if (item != null && !empty) {
                            label.setText(item.name);
                            setGraphic(hbox);
                        }
                    }else {
                        int id=item.groupID;
                        Label label = new Label(chatClient.groupNames.get(id));
                        {
                            hbox.getChildren().add(label);
                            if (chatClient.unreadGroup.containsKey(item.groupID)) {
                                if (chatClient.unreadGroup.get(item.groupID) > 0) {
                                    Circle dot = new Circle(3);
                                    dot.setFill(Color.RED);
                                    hbox.getChildren().add(dot);
                                }
                            }
                            //显示在线群成员
                            Button btn = new Button("在线群成员");
                            btn.setOnAction(event -> {
                                try {
                                    Stage stage = new Stage();
                                    stage.setTitle("群成员列表");
                                    ListView<String> users = new ListView<>();
                                    chatClient.sendMessage("get the user list by: "+id);
                                    Thread.sleep(20);
                                    for (String str:chatClient.s){
                                        if(chatClient.groupMember.containsKey(id)&&chatClient.groupMember.get(id).contains(str)) {
                                            users.getItems().add(str);
                                        }
                                    }
                                    HBox box = new HBox(10);
                                    box.setAlignment(Pos.CENTER);
                                    box.setPadding(new Insets(20, 20, 20, 20));
                                    box.getChildren().add(users);
                                    stage.setScene(new Scene(box));
                                    stage.show();

                                } catch (IOException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            });


                            hbox.getChildren().add(btn);
                            HBox.setHgrow(btn, Priority.ALWAYS); // 设置Circle在HBox中的水平拉伸方式
                            HBox.setMargin(btn, new Insets(0, 0, 0, 80));

                            hbox.setSpacing(10);
                            hbox.setAlignment(Pos.CENTER_LEFT);
                        }

                        setText(null);
                        setGraphic(null);
                        if (item != null && !empty) {
                            label.setText(chatClient.groupNames.get(id));
                            setGraphic(hbox);
                        }
                    }
                }
            };
        }
    }
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    if (msg.isFileType()){
                        HBox wrapper = new HBox();
                        Label nameLabel = new Label(msg.getSentBy());
                        Label msgLabel = new Label();
                        //点击可以下载文件并保存在本地
                        Button loadButton=new Button("\uD83D\uDCE5");
                        loadButton.setOnAction(event -> {
                            String name=msg.getFileName();
                            String type=msg.getFileType();
                            byte[] content=msg.getFileDate();
                            FileChooser fileChooser = new FileChooser();
                            fileChooser.setTitle("Save File");
                            fileChooser.setInitialFileName(name);
                            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(type.toUpperCase() + " files", "*." + type));
                            File selectedFile = fileChooser.showSaveDialog(getScene().getWindow());
                            if (selectedFile != null) {
                                try {
                                    FileOutputStream fos = new FileOutputStream(selectedFile);
                                    fos.write(content);
                                    fos.close();
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "文件发送成功！");
                                    alert.showAndWait();
                                } catch (IOException e) {
                                    Alert alert = new Alert(Alert.AlertType.ERROR, "文件发送失败！");
                                    alert.showAndWait();
                                    e.printStackTrace();
                                }
                            }
                        });
                        StringBuilder sb=new StringBuilder();
                        for (int i=0;i<msg.getFileName().length()+4;i++){
                            sb.append("-");
                        }
                        String s=sb.toString();
                        msgLabel.setText(s+"\n"+"|  "+msg.getFileName()+"  |"+"\n"+"  "+s);
                        nameLabel.setPrefSize(50, 20);
                        nameLabel.setWrapText(true);
                        nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                        if (username.equals(msg.getSentBy())) {
                            wrapper.setAlignment(Pos.TOP_RIGHT);
                            wrapper.getChildren().addAll(msgLabel, nameLabel);
                            msgLabel.setPadding(new Insets(0, 20, 0, 0));
                        } else {
                            wrapper.setAlignment(Pos.TOP_LEFT);
                            wrapper.getChildren().addAll(nameLabel, msgLabel,loadButton);
                            msgLabel.setPadding(new Insets(0, 0, 0, 20));
                        }

                        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                        setGraphic(wrapper);
                        return;
                    }
                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }
}
