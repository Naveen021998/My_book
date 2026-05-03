package com.example.coreapp.party.repository;

import com.example.coreapp.party.entity.PartyEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartyRepository extends JpaRepository<PartyEntity, Long> {

    Optional<PartyEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    long deleteByIdAndOwnerUserId(Long id, Long ownerUserId);

    @Query("""
            select p from PartyEntity p
            where p.ownerUserId = :ownerUserId
              and (:query is null or :query = '' or lower(p.name) like lower(concat('%', :query, '%'))
                   or (p.phone is not null and p.phone like concat('%', :query, '%')))
            """)
    Page<PartyEntity> search(@Param("ownerUserId") long ownerUserId, @Param("query") String query, Pageable pageable);
}
