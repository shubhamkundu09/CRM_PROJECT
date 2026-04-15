package com.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadType leadType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStage leadStage;

    @Column(nullable = false)
    private LocalDate nextFollowUpDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String remarks;

    @Column(nullable = false)
    private String nextFollowUp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id", nullable = false)
    private Employee assignedEmployee;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    private Integer callsMadeCount;
    private Integer meetingsBookedCount;
    private Integer meetingsDoneCount;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    public void initializeCounts() {
        if (callsMadeCount == null) callsMadeCount = 0;
        if (meetingsBookedCount == null) meetingsBookedCount = 0;
        if (meetingsDoneCount == null) meetingsDoneCount = 0;
    }

    @Enumerated(EnumType.STRING)
    private MainService interestedService;

    @Enumerated(EnumType.STRING)
    private ServiceSubcategory serviceSubcategory;

    @Enumerated(EnumType.STRING)
    private ServiceSubSubcategory serviceSubSubcategory;

    @Column(length = 500)
    private String serviceDescription;

    @Version
    private Integer version;

    @Column(nullable = false)
    @Builder.Default
    private Integer updateCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private String lastUpdatedBy = "SYSTEM";

    private LocalDateTime lastContactDate;
}