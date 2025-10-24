   import javafx.application.Application;
   import javafx.application.Platform;
   import javafx.scene.Scene;
   import javafx.scene.control.Label;
   import javafx.stage.Stage;

   public class Teste extends Application {
       public static void main(String[] args) {
           launch(args);
       }

       @Override
       public void start(Stage primaryStage) {
           Label label = new Label("Olá, JavaFX!");
           Scene scene = new Scene(label, 300, 200);
           primaryStage.setScene(scene);
           primaryStage.setTitle("Teste");

           // Configura o comportamento de fechamento
           primaryStage.setOnCloseRequest(event -> {
               Platform.exit();
               System.exit(0);
           });

           primaryStage.show();
       }
   }
