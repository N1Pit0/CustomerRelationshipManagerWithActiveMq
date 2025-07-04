package com.mygym.crm.backstages.domain.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "training_type_table")
@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"trainers", "trainings"})
@ToString(exclude = {"trainers", "trainings"})
public class TrainingType implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "training_type_id")
    private Long trainingTypeId;

    @Column(nullable = false, unique = true)
    private String trainingTypeName;

    @OneToMany(mappedBy = "trainingType", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Trainer> trainers = new HashSet<>();

    @OneToMany(mappedBy = "trainingType", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    private Set<Training> trainings = new HashSet<>();
}
