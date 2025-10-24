package com.factoring.pdf2csv;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class App extends Application {

   

    @Override
    public void start(Stage stage) throws Exception {
     
        try {
            
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/MainView.fxml")); // Verifique o caminho!
            Parent root = fxmlLoader.load();
           
            
            stage.setOnCloseRequest(event -> {
                    Platform.exit(); // Termina a aplicação quando a janela é fechada
                    // event.consume(); // (Opcional) Impede o fechamento padrão, se necessário.
                });

            
            // Icone FrioRio
            Image icon = new Image(getClass().getResourceAsStream("/img/icon.png"));
            stage.getIcons().add(icon);
            
            stage.setResizable(false);
            Scene scene = new Scene(root);
            stage.setTitle("PDFCSV Converter - Contratos FIDCS");
            stage.setScene(scene);
            stage.show();
          
        } catch (Exception e) {
           

            // Opcional: Mostrar um Alert para o usuário
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erro");
            alert.setHeaderText("Erro ao iniciar a aplicação");
            alert.setContentText("Ocorreu um erro ao iniciar a aplicação. Verifique o log para detalhes: " + e.getMessage());
            alert.showAndWait();
        }
      
    }

    public static void main(String[] args) {
     
        launch();
     
    }
}
