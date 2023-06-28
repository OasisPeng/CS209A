package cn.edu.sustech.cs209.chatting.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class Main extends Application {

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        stage.setScene(new Scene(fxmlLoader.load()));
        stage.setTitle("Chatting Client");
        stage.setOnCloseRequest(event -> {
            // 执行保存操作
            try {
                FileOutputStream fos = new FileOutputStream("chatMessages.ser");
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(Controller.getClient().messages); // 将Map对象写入文件中
                oos.close();
                fos.close();
                FileOutputStream fos1 = new FileOutputStream("groupMessages.ser");
                ObjectOutputStream oos1 = new ObjectOutputStream(fos1);
                oos1.writeObject(Controller.getClient().groupMessages); // 将Map对象写入文件中
                oos1.close();
                fos1.close();
                FileOutputStream fos2 = new FileOutputStream("groupNames.ser");
                ObjectOutputStream oos2 = new ObjectOutputStream(fos2);
                oos2.writeObject(Controller.getClient().groupNames); // 将Map对象写入文件中
                oos2.close();
                fos2.close();
                FileOutputStream fos3 = new FileOutputStream("groupMember.ser");
                ObjectOutputStream oos3 = new ObjectOutputStream(fos3);
                oos3.writeObject(Controller.getClient().groupMember); // 将Map对象写入文件中
                oos3.close();
                fos3.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        stage.show();
    }
}
