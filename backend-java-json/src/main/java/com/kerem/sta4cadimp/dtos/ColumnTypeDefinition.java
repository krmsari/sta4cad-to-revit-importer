package com.kerem.sta4cadimp.dtos;

public class ColumnTypeDefinition {
    public String typeLabel;
    public double widthCm;
    public double heightCm;

    public ColumnTypeDefinition(String typeLabel, double widthCm, double heightCm) {
        this.typeLabel = typeLabel;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
    }
}