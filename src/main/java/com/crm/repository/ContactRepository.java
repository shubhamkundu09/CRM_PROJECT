package com.crm.repository;

import com.crm.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    Optional<Contact> findByEmail(String email);

    Optional<Contact> findByPhoneNumber(String phoneNumber);

    @Query("SELECT c FROM Contact c WHERE LOWER(c.email) = LOWER(:email) OR c.phoneNumber = :phoneNumber")
    Optional<Contact> findByEmailIgnoreCaseOrPhoneNumber(@Param("email") String email, @Param("phoneNumber") String phoneNumber);

    List<Contact> findByNameContainingIgnoreCase(String name);

    @Query("SELECT c FROM Contact c WHERE " +
            "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:phone IS NULL OR c.phoneNumber LIKE CONCAT('%', :phone, '%'))")
    Page<Contact> searchContacts(@Param("name") String name,
                                 @Param("email") String email,
                                 @Param("phone") String phone,
                                 Pageable pageable);

    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
}