package tn.mathzero.resource.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Generated lesson + quiz attached to one approved external resource. */
@Entity
@Table(name = "learning_contents")
public class LearningContent {
    @Id
    private UUID id;

    @Column(name = "resource_id", nullable = false, unique = true)
    private UUID resourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExternalResource.Cycle cycle;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "steps_json", nullable = false, columnDefinition = "text")
    private String stepsJson;

    @Column(name = "exercises_json", nullable = false, columnDefinition = "text")
    private String exercisesJson;

    @Column(name = "demo_json", columnDefinition = "text")
    private String demoJson;

    @Column(name = "generator_version", nullable = false)
    private int generatorVersion = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public ExternalResource.Cycle getCycle() { return cycle; }
    public void setCycle(ExternalResource.Cycle cycle) { this.cycle = cycle; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getStepsJson() { return stepsJson; }
    public void setStepsJson(String stepsJson) { this.stepsJson = stepsJson; }
    public String getExercisesJson() { return exercisesJson; }
    public void setExercisesJson(String exercisesJson) { this.exercisesJson = exercisesJson; }
    public String getDemoJson() { return demoJson; }
    public void setDemoJson(String demoJson) { this.demoJson = demoJson; }
    public int getGeneratorVersion() { return generatorVersion; }
    public void setGeneratorVersion(int generatorVersion) { this.generatorVersion = generatorVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
