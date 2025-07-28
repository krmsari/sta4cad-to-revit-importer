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
@Table(name = "slabs")
public class Slab {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String label;
    private double thicknessCm;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "slab_boundary_axes", joinColumns = @JoinColumn(name = "slab_id"))
    @Column(name = "axis_ref")
    private List<String> boundaryAxisRefs = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id")
    @JsonBackReference
    private Floor floor;
}
