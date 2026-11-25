package tn.mathzero.resource.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import tn.mathzero.resource.domain.ExternalResource;

public interface ExternalResourceRepository
        extends JpaRepository<ExternalResource, UUID>, JpaSpecificationExecutor<ExternalResource> {
    Optional<ExternalResource> findByUrl(String url);
}
