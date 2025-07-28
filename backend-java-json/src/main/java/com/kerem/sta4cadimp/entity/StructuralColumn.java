package com.kerem.sta4cadimp.entity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "structural_columns") // "column" SQL anahtar kelimesi olabileceğinden "structural_columns" kullanıldı
public class StructuralColumn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String st4Sid; // .ST4 dosyasındaki S_ID
    private String st4Aid; // .ST4 dosyasındaki A_ID
    private String typeLabel; //Kolon tipi etiketi (örn: "101")

    private double dimensionWidthCm; // Genişlik (cm)
    private double dimensionHeightCm; // Yükseklik (cm)

    private double positionXMetre; // Nihai X pozisyonu (metre)
    private double positionYMetre; // Nihai Y pozisyonu (metre)

    private double offsetXmm; // X kaçıklığı (mm)
    private double offsetYmm; // Y kaçıklığı (mm)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id")
    @JsonBackReference
    private Floor floor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @JsonBackReference
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nominal_x_axis_id")
    @JsonBackReference
    private Axis nominalXAxis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nominal_y_axis_id")
    @JsonBackReference
    private Axis nominalYAxis;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StructuralColumn that = (StructuralColumn) o;
        return Objects.equals(id, that.id) && Objects.equals(st4Sid, that.st4Sid) && Objects.equals(st4Aid, that.st4Aid) && Objects.equals(floor, that.floor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, st4Sid, st4Aid, floor);
    }
}