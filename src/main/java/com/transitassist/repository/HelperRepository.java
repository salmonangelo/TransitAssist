package com.transitassist.repository;

import com.transitassist.entity.Helper;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HelperRepository extends JpaRepository<Helper, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Helper> findByActiveTrueAndAvailableTrue();

    /**
     * Loads the helper row with a database lock (SELECT ... FOR UPDATE).
     * Two staff members assigning the same helper at the same moment are therefore
     * processed one after the other, so the overlap check cannot be bypassed.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Helper h where h.id = :id")
    Optional<Helper> findByIdForUpdate(@Param("id") Long id);
}
