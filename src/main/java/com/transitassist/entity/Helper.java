package com.transitassist.entity;

import com.transitassist.enums.AssistanceType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "helpers",
        uniqueConstraints = @UniqueConstraint(name = "uk_helpers_email", columnNames = "email"))
public class Helper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    /** Stored in a separate table: helper_skills(helper_id, skill). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "helper_skills", joinColumns = @JoinColumn(name = "helper_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "skill", nullable = false, length = 30)
    private Set<AssistanceType> assistanceSkills = EnumSet.noneOf(AssistanceType.class);

    @Column(nullable = false)
    private boolean available = true;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** One helper can have many assistance requests. */
    @OneToMany(mappedBy = "helper")
    private List<AssistanceRequest> requests = new ArrayList<>();

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Set<AssistanceType> getAssistanceSkills() { return assistanceSkills; }
    public void setAssistanceSkills(Set<AssistanceType> skills) {
        this.assistanceSkills.clear();
        if (skills != null) {
            this.assistanceSkills.addAll(skills);
        }
    }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<AssistanceRequest> getRequests() { return requests; }
}
