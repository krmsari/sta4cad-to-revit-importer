package com.kerem.sta4cadimp.dtos;

public class RawColumnData {
    public int st4FloorNum;
    public String sId;
    public String aId;
    public double offsetXmm;
    public double offsetYmm;

    public RawColumnData(int st4FloorNum, String sId, String aId, double offsetXmm, double offsetYmm) {
        this.st4FloorNum = st4FloorNum;
        this.sId = sId;
        this.aId = aId;
        this.offsetXmm = offsetXmm;
        this.offsetYmm = offsetYmm;
    }
}