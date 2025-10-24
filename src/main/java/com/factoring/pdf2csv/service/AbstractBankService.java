package com.factoring.pdf2csv.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.factoring.pdf2csv.view.ProcessingWindow;

public abstract class AbstractBankService implements BankService {

    ProcessingWindow processingWindow;

    TituloInfo info = null;
    String url = "jdbc:postgresql://10.147.18.2:5432/GIX"; // URL de conexão para PostgreSQL
    String user = "frioriobi";
    String password = "FR5aB2ClHG3i";

    public TituloInfo getTituloInfo(String titulo, int codigoBanco) {
        if (titulo == null || titulo.trim().isEmpty()) {
            return null;
        }

        try {
            // Converter o título para int
            int tituloInt = Integer.parseInt(titulo);

            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                String query = "SELECT crecempe, crecparc, crecseri "
                        + "FROM arqcrec "
                        + "WHERE crecdocu = ? "
                        + "AND crecbanc = ? "
                        + "ORDER BY crecemis DESC "
                        + "LIMIT 1";
                String formattedQuery = getFormattedQuery(query, tituloInt);

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, tituloInt);
                    stmt.setInt(2, codigoBanco);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int codigoEmpresa = rs.getInt("crecempe");
                            String codigoParcela = rs.getString("crecparc");
                            String serie = rs.getString("crecseri");

                            info = new TituloInfo(codigoEmpresa, codigoParcela, serie, titulo);
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
//            System.out.println("Erro: O parâmetro 'titulo' não é um número válido.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Erro ao conectar ao banco de dados ou executar a consulta: " + e.getMessage());
            e.printStackTrace();
        }

        return info;
    }

    public TituloInfo getTituloInfoByParcela(String titulo, int parcela, int codigoBanco) {
        if (titulo == null || titulo.trim().isEmpty()) {
            return null;
        }

        try {
            // Converter o título para int
            int tituloInt = Integer.parseInt(titulo);


            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                String query = "SELECT EMPRESA, PARCELA, SERIE, EMISSAO "
                + "FROM ( "+
                        " SELECT crecempe AS EMPRESA, crecparc AS PARCELA, crecseri AS SERIE, CRECEMIS as EMISSAO " +
                        "FROM arqcrec "+
                        "WHERE crecdocu = ? "+
                        "AND CRECPARC = ? "+
                        "AND crecbanc = ? "+
                "UNION ALL "+
                        "SELECT BRECEMPE AS EMPRESA, BRECPARC AS PARCELA, BRECSERI AS SERIE, BRECEMIS as EMISSAO "+
                        "FROM ARQBREC "+
                        "WHERE BRECDOCU = ? "+
                        "AND BRECPARC = ? "+
                        "AND BRECBANC = ? "+
                ") AS subquery " +
                "ORDER BY EMISSAO desc limit 1;";

                String formattedQuery = getFormattedQuery(query, tituloInt);

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, tituloInt);
                    stmt.setInt(2, parcela);
                    stmt.setInt(3, codigoBanco);
                    stmt.setInt(4,tituloInt );
                    stmt.setInt(5, parcela);
                    stmt.setInt(6, codigoBanco);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int codigoEmpresa = rs.getInt("EMPRESA");
                            String codigoParcela = rs.getString("PARCELA");
                            String serie = rs.getString("SERIE");

                            info = new TituloInfo(codigoEmpresa, codigoParcela, serie, titulo);
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
//            System.out.println("Erro: O parâmetro 'titulo' não é um número válido.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Erro ao conectar ao banco de dados ou executar a consulta: " + e.getMessage());
            e.printStackTrace();
        }

        return info;
    }

    private String getFormattedQuery(String query, Object... params) {
        for (Object param : params) {
            if (param instanceof String) {
                query = query.replaceFirst("\\?", "'" + param + "'");
            } else {
                query = query.replaceFirst("\\?", param.toString());
            }
        }
        return query;
    }

    public TituloInfo getTituloByNbco(String sequencial, int codigoBanco) {
        info = null;
//        System.out.println("com.factoring.pdf2csv.service.AbstractBankService.getTituloByNbco()");

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            String query = "SELECT crecempe, crecdocu ,crecparc, crecseri " +
                    "FROM arqcrec " +
                    "WHERE crecbanc = ? " +
                    "AND crecnbco  = ?";
//            System.out.println("depois da query");
            String formattedQuery = getFormattedQuery(query, sequencial);
//            System.out.println(formattedQuery);
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, codigoBanco);
                stmt.setString(2, sequencial.trim());

//                System.out.println("Result set");
                try (ResultSet rs = stmt.executeQuery()) {
                    System.out.println("Dentro do resultset");
                    if (rs.next()) {
                        System.out.println("Denntro do next");
                        int codigoEmpresa = rs.getInt("crecempe");
                        String titulo = rs.getString("crecdocu");
                        String codigoParcela = rs.getString("crecparc");
                        String serie = rs.getString("crecseri");

                        info = new TituloInfo(codigoEmpresa, codigoParcela, serie, titulo);
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Erro ao executar transação db: " + e.getMessage());
            e.printStackTrace();
        }

        return info;
    }

    public TituloInfo getInfobyTituloVencimento(String titulo, String dataVencimento) {
        info = null;

        try {
            // Converter o título para int
            int tituloInt = Integer.parseInt(titulo);

            // Formato correto para "17/10/2025"
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            java.util.Date utilDate = sdf.parse(dataVencimento);
            java.sql.Date dataVenc = new java.sql.Date(utilDate.getTime());

            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                String query = "SELECT crecempe, crecparc, crecseri "
                        + "FROM arqcrec "
                        + "WHERE crecdocu = ? "
                        + "AND crecvenc = ? "
                        + "ORDER BY crecemis DESC "
                        + "LIMIT 1";
                String formattedQuery = getFormattedQuery(query, tituloInt);

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, tituloInt);
                    stmt.setDate(2, dataVenc);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int codigoEmpresa = rs.getInt("crecempe");
                            String codigoParcela = rs.getString("crecparc");
                            String serie = rs.getString("crecseri");

                            info = new TituloInfo(codigoEmpresa, codigoParcela, serie, titulo);
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
//            System.out.println("Erro: O parâmetro 'titulo' não é um número válido.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Erro ao conectar ao banco de dados ou executar a consulta: " + e.getMessage());
            e.printStackTrace();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        return info;
    }
}
