package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ChatView.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("ChatFX - Cliente (Phase2)") ;
        stage.setScene(scene);
        stage.setWidth(700);
        stage.setHeight(500);
        stage.show();
    }
    public static void main(String[] args) { launch(); }
}
