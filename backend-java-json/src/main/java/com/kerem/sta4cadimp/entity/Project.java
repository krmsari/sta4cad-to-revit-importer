package com.kerem.sta4cadimp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String projectTitle;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Floor> floors = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Axis> axes = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<StructuralColumn> columns = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Beam> beams = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("project-panel")
    @JsonIgnore
    private List<Panel> panels = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<FoundationSlab> foundationSlabs = new ArrayList<>();


    public void addFloor(Floor floor) {
        floors.add(floor);
        floor.setProject(this);
    }
    public void addAxis(Axis axis) {
        axes.add(axis);
        axis.setProject(this);
    }
    public void addStructuralColumn(StructuralColumn column) {
        columns.add(column);
        column.setProject(this);
    }
    public void addBeam(Beam beam) {
        beams.add(beam);
        beam.setProject(this);
    }
    public void addFoundationSlab(FoundationSlab foundationSlab) {
        foundationSlabs.add(foundationSlab);
        foundationSlab.setProject(this);
    }
    public void addPanel(Panel panel) {
        panels.add(panel);
        panel.setProject(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(id, project.id) && Objects.equals(fileName, project.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fileName);
    }
}