package tn.mathzero.curriculum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "skills",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_skill_code",
                columnNames = {"curriculum_version_id", "code"}))
public class Skill {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curriculum_version_id", nullable = false)
    private CurriculumVersion curriculumVersion;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(name = "title_fr", nullable = false, length = 240)
    private String titleFr;

    @Column(name = "title_ar", nullable = false, length = 240)
    private String titleAr;

    @Column(name = "objective_fr", nullable = false)
    private String objectiveFr;

    @Column(name = "objective_ar", nullable = false)
    private String objectiveAr;

    @ManyToMany(mappedBy = "skills")
    private Set<Chapter> chapters = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
            name = "skill_prerequisites",
            joinColumns = @JoinColumn(name = "skill_id"),
            inverseJoinColumns = @JoinColumn(name = "prerequisite_skill_id"))
    private Set<Skill> prerequisites = new LinkedHashSet<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public CurriculumVersion getCurriculumVersion() { return curriculumVersion; }
    public void setCurriculumVersion(CurriculumVersion curriculumVersion) { this.curriculumVersion = curriculumVersion; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitleFr() { return titleFr; }
    public void setTitleFr(String titleFr) { this.titleFr = titleFr; }
    public String getTitleAr() { return titleAr; }
    public void setTitleAr(String titleAr) { this.titleAr = titleAr; }
    public String getObjectiveFr() { return objectiveFr; }
    public void setObjectiveFr(String objectiveFr) { this.objectiveFr = objectiveFr; }
    public String getObjectiveAr() { return objectiveAr; }
    public void setObjectiveAr(String objectiveAr) { this.objectiveAr = objectiveAr; }
    public Set<Skill> getPrerequisites() { return prerequisites; }
}
