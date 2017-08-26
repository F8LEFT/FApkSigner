package f8left;

import com.android.apksigner.ApkSignerTool;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("MainWindow.fxml"));
        primaryStage.setTitle("FApkSigner");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }


    public static void main(String[] args) throws Exception{
        boolean launchUi = false;
        if(args.length == 0 ) launchUi = true;
        else if(args[0].equals("launch")) launchUi = true;
        if(launchUi) launch(args);
        else ApkSignerTool.main(args);
    }
}
