package com.transitassist.entity;

import com.transitassist.enums.AssistanceType;
import com.transitassist.enums.RequestStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "assistance_requests", indexes = {
        @Index(name = "idx_ar_user", columnList = "user_id"),
        @Index(name = "idx_ar_helper", columnList = "helper_id"),
        @Index(name = "idx_ar_trip_date", columnList = "trip_date"),
        @Index(name = "idx_ar_status", columnList = "status")
})
public class AssistanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Every request belongs to exactly one user. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Optional: empty until staff assigns a helper. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "helper_id")
    private Helper helper;

    @Column(name = "pickup_point", nullable = false, length = 150)
    private String pickupPoint;

    @Column(nullable = false, length = 150)
    private String destination;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @Column(name = "trip_time", nullable = false)
    private LocalTime tripTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "assistance_type", nullable = false, length = 30)
    private AssistanceType assistanceType;

    @Column(name = "additional_notes", length = 500)
    private String additionalNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.REQUESTED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Helper getHelper() { return helper; }
    public void setHelper(Helper helper) { this.helper = helper; }
    public String getPickupPoint() { return pickupPoint; }
    public void setPickupPoint(String pickupPoint) { this.pickupPoint = pickupPoint; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalDate getTripDate() { return tripDate; }
    public void setTripDate(LocalDate tripDate) { this.tripDate = tripDate; }
    public LocalTime getTripTime() { return tripTime; }
    public void setTripTime(LocalTime tripTime) { this.tripTime = tripTime; }
    public AssistanceType getAssistanceType() { return assistanceType; }
    public void setAssistanceType(AssistanceType assistanceType) { this.assistanceType = assistanceType; }
    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
