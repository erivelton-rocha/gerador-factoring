package com.factoring.pdf2csv.service;

public class TituloInfo {
    private int codigoEmpresa;
    private String codigoParcela;
    private String serie;
    private String titulo;

    public TituloInfo(int codigoEmpresa, String codigoParcela, String serie, String titulo) {
        this.codigoEmpresa = codigoEmpresa;
        this.codigoParcela = codigoParcela;
        this.serie = serie;
        this.titulo = titulo;
    }

    public int getCodigoEmpresa() {
        return codigoEmpresa;
    }

    public String getCodigoParcela() {
        return codigoParcela;
    }

    public String getSerie() {
        return serie;
    }

    public String getTitulo() {
        return titulo;
    }

       
    @Override
    public String toString() {
        return "TituloInfo{" + "codigoEmpresa=" + codigoEmpresa + ", codigoParcela=" + codigoParcela + ", serie=" + serie + ", titulo=" + titulo + '}';
    }


}
