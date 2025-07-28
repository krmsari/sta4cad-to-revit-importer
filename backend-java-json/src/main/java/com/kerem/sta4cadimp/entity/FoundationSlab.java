package com.kerem.sta4cadimp.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "foundation_slabs")
public class FoundationSlab {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String label;
    private double thicknessCm;
    private double elevationMetre; //Temel üst kotu

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "foundation_slab_boundary_axes", joinColumns = @JoinColumn(name = "foundation_slab_id"))
    @Column(name = "axis_ref")
    private List<String> boundaryAxisRefs = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @JsonBackReference
    private Project project;
}