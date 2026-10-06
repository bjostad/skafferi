package com.skafferi.repository;

import com.skafferi.domain.ExternalMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExternalMappingRepository extends JpaRepository<ExternalMapping, String> {

    Optional<ExternalMapping> findBySystemTypeAndExternalKey(String systemType, String externalKey);

    List<ExternalMapping> findByItemId(String itemId);
}
