package com.kerem.sta4cadimp.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "beams")
public class Beam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String label;
    private double widthCm;
    private double heightCm;
    private String propertyCode;

    private String planeAxisRef;
    private String startSpanAxisRef;
    private String endSpanAxisRef;

    private double startXMetre;
    private double startYMetre;
    private double endXMetre;
    private double endYMetre;

    private Double startZOffsetCm;
    private Double endZOffsetCm;

    private Double wallThicknessCm;
    private Double wallHeightCm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id")
    @JsonBackReference
    private Floor floor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @JsonBackReference
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_column_id")
    @JsonBackReference
    private StructuralColumn startColumn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_column_id")
    @JsonBackReference
    private StructuralColumn endColumn;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Beam beam = (Beam) o;
        return Objects.equals(id, beam.id) && Objects.equals(label, beam.label) && Objects.equals(floor, beam.floor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, floor);
    }
}