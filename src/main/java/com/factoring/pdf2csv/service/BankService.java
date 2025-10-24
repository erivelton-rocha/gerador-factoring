package com.factoring.pdf2csv.service;

import java.io.File;

public interface BankService {
    String extractAndProcess(File txtFile, double totalValue, double totalPaid, double iofRate, double fee, int bankCode);
    String getEmpresa(String titulo); // Declarar o método na interface
    void startProcessingWindow();
    void updateProcessingWindow(String message);
    void closeProcessingWindow();
}
