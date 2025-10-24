package com.factoring.pdf2csv.controller;

import java.io.File;

import com.factoring.pdf2csv.model.BankEnum;
import com.factoring.pdf2csv.service.BankService;
import com.factoring.pdf2csv.service.BankServiceFactory;
import com.factoring.pdf2csv.util.PdfToTextUtil;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private TextField filePathField;
    @FXML
    private ComboBox<BankEnum> bankComboBox;
    @FXML
    private TextField totalValueField;
    @FXML
    private TextField totalPaidField;
    @FXML
    private TextField iofRateField;
    @FXML
    private TextField feeField;
    @FXML
    private Label statusLabel;

    private File selectedFile;

    @FXML
    private void initialize() {
        logger.info("Inicializando MainController...");
        bankComboBox.getItems().addAll(BankEnum.values());
        statusLabel.setMinHeight(50);
        statusLabel.setWrapText(true);
        logger.info("MainController inicializado.");
    }

    @FXML
    private void selectPdfFile() {
        logger.info("Abrindo FileChooser...");
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos PDF", "*.pdf"));
        selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            logger.info("Arquivo selecionado: {}", selectedFile.getAbsolutePath());
            filePathField.setText(selectedFile.getAbsolutePath());
        } else {
            logger.info("Nenhum arquivo selecionado.");
        }
    }

    @FXML
    private void convertToTxt() {
        if (selectedFile == null || bankComboBox.getValue() == null) {
            statusLabel.setText("Selecione um arquivo PDF e um banco.");
            return;
        }

        logger.info("Iniciando conversão para TXT...");

        try {
            File txtFile = new File(selectedFile.getParent(), "dados_extraidos.txt");
            BankEnum selectedBank = bankComboBox.getValue(); // Assumindo que o ComboBox retorna BankEnum
            BankService service = BankServiceFactory.getService(selectedBank);
            int bankCode = selectedBank.getCode();
            //BankService service = BankServiceFactory.getService(bankComboBox.getValue());
            logger.info("Serviço obtido para o banco: {}-{}", selectedBank, bankCode);

                PdfToTextUtil.saveToTextFile(selectedFile, txtFile, bankCode); // Usa o método aprimorad


            logger.info("Arquivo TXT gerado: {}", txtFile.getAbsolutePath());
            statusLabel.setText("Arquivo TXT gerado: " + txtFile.getAbsolutePath());

            if (totalValueField.getText() == null || totalValueField.getText().trim().isEmpty()
                    || totalPaidField.getText() == null || totalPaidField.getText().trim().isEmpty()) {
                logger.info("Campos não preenchidos.");
                statusLabel.setText("Campos não preenchidos.");
                return;
            }

            double totalValue = Double.parseDouble(totalValueField.getText().replace(".", "").replace(",", "."));
            double totalPaid = Double.parseDouble(totalPaidField.getText().replace(".", "").replace(",", "."));

            // Verificar se os valores estão dentro de faixas válidas
            if (totalValue <= 0) {
                logger.info("O valor total deve ser maior que zero.");
                statusLabel.setText("O valor total deve ser maior que zero.");
                return;

            }

            if (totalPaid < 0 || totalPaid > totalValue) {
                logger.info("O valor pago deve estar entre 0 e o valor total.");
                statusLabel.setText("O valor pago deve estar entre 0 e o valor total.");
                return;

            }

            double iofRate = parseDoubleOrDefault(iofRateField.getText(), 0.0);
            double fee = parseDoubleOrDefault(feeField.getText(), 0.0);

            String csvFilePath = service.extractAndProcess(txtFile, totalValue, totalPaid, iofRate, fee, bankCode);

            if (csvFilePath != null) {
                logger.info("Arquivo CSV gerado: {}", csvFilePath);
                statusLabel.setText("Dados extraídos e processados com sucesso.");
                clearFields();
            } else {
                logger.error("Erro ao processar dados (csvFilePath é nulo).");
                statusLabel.setText("Erro ao processar dados.");
            }

        } catch (Exception e) {
            logger.error("Erro ao converter PDF para TXT:", e);
            statusLabel.setText("Erro ao converter PDF para TXT: " + e.getMessage());
        }
    }

    private double parseDoubleOrDefault(String text, double defaultValue) {
        try {
            if (text.isEmpty()) {
                return defaultValue;
            }
            String normalizedText = text.replace(".", "").replace(",", ".");
            return Double.parseDouble(normalizedText);
        } catch (NumberFormatException e) {
            logger.warn("Erro ao converter número (usando valor padrão): {}", text, e);
            return defaultValue;
        }
    }

    private void clearFields() {
        filePathField.clear();
        bankComboBox.setValue(null);
        totalValueField.clear();
        totalPaidField.clear();
        iofRateField.clear();
        feeField.clear();
        //statusLabel.setText("");
        selectedFile = null;
    }

}
