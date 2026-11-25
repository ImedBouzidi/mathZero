package tn.mathzero.curriculum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chapters",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_chapter_code",
                columnNames = {"subject_id", "code"}))
public class Chapter {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(name = "title_fr", nullable = false, length = 180)
    private String titleFr;

    @Column(name = "title_ar", nullable = false, length = 180)
    private String titleAr;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @ManyToMany
    @JoinTable(
            name = "chapter_skills",
            joinColumns = @JoinColumn(name = "chapter_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    @OrderColumn(name = "display_order")
    private List<Skill> skills = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitleFr() { return titleFr; }
    public void setTitleFr(String titleFr) { this.titleFr = titleFr; }
    public String getTitleAr() { return titleAr; }
    public void setTitleAr(String titleAr) { this.titleAr = titleAr; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public List<Skill> getSkills() { return skills; }
}
