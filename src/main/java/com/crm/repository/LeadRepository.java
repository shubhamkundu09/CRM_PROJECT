package com.crm.repository;

import com.crm.entity.Lead;
import com.crm.entity.LeadStage;
import com.crm.entity.LeadType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    List<Lead> findByAssignedEmployeeId(Long employeeId);

    List<Lead> findByLeadType(LeadType leadType);

    List<Lead> findByLeadStage(LeadStage leadStage);

    List<Lead> findByNextFollowUpDate(LocalDate date);

    List<Lead> findByNextFollowUpDateBefore(LocalDate date);

    List<Lead> findByIsActiveTrue();

    @Query("SELECT l FROM Lead l WHERE l.assignedEmployee.id = :employeeId AND l.nextFollowUpDate = :date")
    List<Lead> findFollowUpsByEmployeeAndDate(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    @Query("SELECT l FROM Lead l WHERE l.leadStage IN :stages")
    List<Lead> findByLeadStages(@Param("stages") List<LeadStage> stages);

    boolean existsByEmail(String email);

    long countByLeadType(LeadType leadType);

    long countByLeadStage(LeadStage leadStage);

    @Query("SELECT l.leadStage, COUNT(l) FROM Lead l GROUP BY l.leadStage")
    List<Object[]> countLeadsByStage();

    @Query("SELECT l.leadType, COUNT(l) FROM Lead l GROUP BY l.leadType")
    List<Object[]> countLeadsByType();

    // ADD THIS NEW SEARCH METHOD
    @Query("""
        SELECT l FROM Lead l 
        WHERE 
            (:name IS NULL OR TRIM(:name) = '' OR LOWER(l.name) LIKE LOWER(CONCAT('%', TRIM(:name), '%')))
        AND (:email IS NULL OR TRIM(:email) = '' OR LOWER(l.email) LIKE LOWER(CONCAT('%', TRIM(:email), '%')))
        AND (:phone IS NULL OR TRIM(:phone) = '' OR l.phoneNumber LIKE CONCAT('%', TRIM(:phone), '%'))
        AND (:leadType IS NULL OR l.leadType = :leadType)
        AND (:leadStage IS NULL OR l.leadStage = :leadStage)
        AND (:assignedEmployeeId IS NULL OR l.assignedEmployee.id = :assignedEmployeeId)
        AND (:isActive IS NULL OR l.isActive = :isActive)
        AND (:source IS NULL OR TRIM(:source) = '' OR LOWER(l.source) LIKE LOWER(CONCAT('%', TRIM(:source), '%')))
        AND (:nextFollowUpDate IS NULL OR l.nextFollowUpDate = :nextFollowUpDate)
        AND (CAST(:followUpFrom AS date) IS NULL OR l.nextFollowUpDate >= :followUpFrom)
        AND (CAST(:followUpTo AS date) IS NULL OR l.nextFollowUpDate <= :followUpTo)
        AND (CAST(:createdFrom AS date) IS NULL OR DATE(l.createdAt) >= :createdFrom)
        AND (CAST(:createdTo AS date) IS NULL OR DATE(l.createdAt) <= :createdTo)
        AND (CAST(:updatedFrom AS date) IS NULL OR DATE(l.updatedAt) >= :updatedFrom)
        AND (CAST(:updatedTo AS date) IS NULL OR DATE(l.updatedAt) <= :updatedTo)
        AND (:minCallsMade IS NULL OR l.callsMadeCount >= :minCallsMade)
        AND (:maxCallsMade IS NULL OR l.callsMadeCount <= :maxCallsMade)
        AND (:minMeetingsBooked IS NULL OR l.meetingsBookedCount >= :minMeetingsBooked)
        AND (:maxMeetingsBooked IS NULL OR l.meetingsBookedCount <= :maxMeetingsBooked)
        AND (:minMeetingsDone IS NULL OR l.meetingsDoneCount >= :minMeetingsDone)
        AND (:maxMeetingsDone IS NULL OR l.meetingsDoneCount <= :maxMeetingsDone)
        """)
    Page<Lead> searchLeads(
            @Param("name") String name,
            @Param("email") String email,
            @Param("phone") String phone,
            @Param("leadType") LeadType leadType,
            @Param("leadStage") LeadStage leadStage,
            @Param("assignedEmployeeId") Long assignedEmployeeId,
            @Param("isActive") Boolean isActive,
            @Param("source") String source,
            @Param("nextFollowUpDate") LocalDate nextFollowUpDate,
            @Param("followUpFrom") LocalDate followUpFrom,
            @Param("followUpTo") LocalDate followUpTo,
            @Param("createdFrom") LocalDate createdFrom,
            @Param("createdTo") LocalDate createdTo,
            @Param("updatedFrom") LocalDate updatedFrom,
            @Param("updatedTo") LocalDate updatedTo,
            @Param("minCallsMade") Integer minCallsMade,
            @Param("maxCallsMade") Integer maxCallsMade,
            @Param("minMeetingsBooked") Integer minMeetingsBooked,
            @Param("maxMeetingsBooked") Integer maxMeetingsBooked,
            @Param("minMeetingsDone") Integer minMeetingsDone,
            @Param("maxMeetingsDone") Integer maxMeetingsDone,
            Pageable pageable
    );
}