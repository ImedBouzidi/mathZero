package tn.mathzero.curriculum.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import tn.mathzero.curriculum.domain.Curriculum;
import tn.mathzero.curriculum.domain.CurriculumVersion;

public interface CurriculumRepository extends JpaRepository<Curriculum, UUID> {
    Optional<Curriculum> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Curriculum c where c.id = :id")
    Optional<Curriculum> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select distinct c from Curriculum c join c.versions v
            where v.status = :status
              and v.effectiveFrom <= :asOf
              and (v.effectiveTo is null or v.effectiveTo >= :asOf)
              and (:search = ''
                   or lower(c.code) like lower(concat('%', :search, '%'))
                   or lower(c.nameFr) like lower(concat('%', :search, '%'))
                   or lower(c.nameAr) like lower(concat('%', :search, '%')))
            """)
    Page<Curriculum> findPublishedOn(
            @Param("status") CurriculumVersion.Status status,
            @Param("asOf") LocalDate asOf,
            @Param("search") String search,
            Pageable pageable);
}
