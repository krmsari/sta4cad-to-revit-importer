package com.kerem.sta4cadimp.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "floors")
public class Floor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int originalNumber;
    private double elevation;
    private double height;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @JsonBackReference
    private Project project;

    @OneToMany(mappedBy = "floor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<StructuralColumn> columns = new ArrayList<>();

    @OneToMany(mappedBy = "floor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Beam> beams = new ArrayList<>();

    @OneToMany(mappedBy = "floor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Slab> slabs = new ArrayList<>();

    @OneToMany(mappedBy = "floor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("floor-panel")
    private List<Panel> panels = new ArrayList<>();


    public void addStructuralColumn(StructuralColumn column) {
        columns.add(column);
        column.setFloor(this);
    }

    public void addBeam(Beam beam) {
        beams.add(beam);
        beam.setFloor(this);
    }

    public void addSlab(Slab slab) {
        slabs.add(slab);
        slab.setFloor(this);
    }

    public void addPanel(Panel panel) {
        panels.add(panel);
        panel.setFloor(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Floor floor = (Floor) o;
        return originalNumber == floor.originalNumber && Objects.equals(name, floor.name) && Objects.equals(project, floor.project);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, originalNumber, project);
    }
}