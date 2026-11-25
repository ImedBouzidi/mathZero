package tn.mathzero.curriculum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "school_levels",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_school_level_code",
                columnNames = {"curriculum_version_id", "code"}))
public class SchoolLevel {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curriculum_version_id", nullable = false)
    private CurriculumVersion version;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(name = "name_fr", nullable = false, length = 180)
    private String nameFr;

    @Column(name = "name_ar", nullable = false, length = 180)
    private String nameAr;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public CurriculumVersion getVersion() { return version; }
    public void setVersion(CurriculumVersion version) { this.version = version; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameFr() { return nameFr; }
    public void setNameFr(String nameFr) { this.nameFr = nameFr; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
