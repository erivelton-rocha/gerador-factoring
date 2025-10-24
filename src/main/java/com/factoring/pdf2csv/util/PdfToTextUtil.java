package com.factoring.pdf2csv.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PdfToTextUtil {

    // Lista de códigos de bancos especiais
    private static final List<Integer> BANCOS_ESPECIAIS = Arrays.asList(101, 202, 303);

    /**
     * Converte um PDF para texto de acordo com o código do banco.
     * 
     * @param pdfFile  O arquivo PDF a ser processado.
     * @param bankCode Código do banco, indicando qual método de extração usar.
     * @return O texto extraído do PDF.
     * @throws PdfToTextException Se ocorrer um erro na extração.
     */
    public static String convertPdfToText(File pdfFile, int bankCode) throws PdfToTextException {
        if (pdfFile == null) {
            throw new IllegalArgumentException("O arquivo PDF não pode ser nulo.");
        }

        if (BANCOS_ESPECIAIS.contains(bankCode)) {
            return convertPdfToTextByClipboardSimulation(pdfFile);
        } else {
            return convertPdfToTextDefault(pdfFile);
        }
    }

    /**
     * Método de extração padrão - mantém estrutura tradicional.
     */
    private static String convertPdfToTextDefault(File pdfFile) throws PdfToTextException {
        if (!pdfFile.exists()) {
            throw new PdfToTextException("O arquivo PDF não existe: " + pdfFile.getAbsolutePath());
        }

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setSortByPosition(true);

            return pdfStripper.getText(document);
        } catch (IOException e) {
            throw new PdfToTextException("Erro ao processar o PDF: " + pdfFile.getAbsolutePath(), e);
        }
    }

    /**
     * Método de extração simulando "copiar e colar" - para bancos que precisam de melhor formatação.
     */
    public static String convertPdfToTextByClipboardSimulation(File pdfFile) throws PdfToTextException {
        if (!pdfFile.exists()) {
            throw new PdfToTextException("O arquivo PDF não existe: " + pdfFile.getAbsolutePath());
        }

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setSortByPosition(true);
            pdfStripper.setWordSeparator(" ");
            pdfStripper.setParagraphEnd("\n\n");
            pdfStripper.setLineSeparator("\n");
            return pdfStripper.getText(document);
        } catch (IOException e) {
            throw new PdfToTextException("Erro ao processar o PDF: " + pdfFile.getAbsolutePath(), e);
        }
    }

    /**
     * Salva o texto extraído no arquivo TXT.
     */
    public static void saveToTextFile(File pdfFile, File outputFile, int bankCode) throws PdfToTextException {
        if (pdfFile == null || outputFile == null) {
            throw new IllegalArgumentException("O arquivo PDF e o arquivo de saída não podem ser nulos.");
        }

        try {
            String extractedText = convertPdfToText(pdfFile, bankCode);
            System.out.println(extractedText);
            Files.write(outputFile.toPath(), extractedText.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new PdfToTextException("Erro ao salvar o texto no arquivo: " + outputFile.getAbsolutePath(), e);
        }
    }

    /**
     * Exceção personalizada para erros na conversão de PDF.
     */
    static class PdfToTextException extends Exception {
        public PdfToTextException(String message, Throwable cause) {
            super(message, cause);
        }
        public PdfToTextException(String message) {
            super(message);
        }
    }
}
