package com.kerem.sta4cadimp.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "panels")
public class Panel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String label;
    private double widthCm;
    private double heightCm;
    private String propertyCode;

    private double startXMetre;
    private double startYMetre;
    private double endXMetre;
    private double endYMetre;

    private Double startZOffsetCm;
    private Double endZOffsetCm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id")
    @JsonBackReference("floor-panel")
    private Floor floor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @JsonBackReference("project-panel")
    private Project project;
}
