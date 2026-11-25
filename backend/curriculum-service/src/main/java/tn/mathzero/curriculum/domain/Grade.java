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
@Table(name = "grades",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_grade_code",
                columnNames = {"school_level_id", "code"}))
public class Grade {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_level_id", nullable = false)
    private SchoolLevel schoolLevel;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(name = "name_fr", nullable = false, length = 180)
    private String nameFr;

    @Column(name = "name_ar", nullable = false, length = 180)
    private String nameAr;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @ManyToMany
    @JoinTable(
            name = "grade_specializations",
            joinColumns = @JoinColumn(name = "grade_id"),
            inverseJoinColumns = @JoinColumn(name = "specialization_id"))
    private Set<Specialization> specializations = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(
            name = "grade_subjects",
            joinColumns = @JoinColumn(name = "grade_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id"))
    private Set<Subject> subjects = new LinkedHashSet<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public SchoolLevel getSchoolLevel() { return schoolLevel; }
    public void setSchoolLevel(SchoolLevel schoolLevel) { this.schoolLevel = schoolLevel; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameFr() { return nameFr; }
    public void setNameFr(String nameFr) { this.nameFr = nameFr; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public Set<Specialization> getSpecializations() { return specializations; }
    public Set<Subject> getSubjects() { return subjects; }
}
