package com.example.coreapp.party.repository;

import com.example.coreapp.party.entity.PartyBalanceEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartyBalanceRepository extends JpaRepository<PartyBalanceEntity, Long> {

    Optional<PartyBalanceEntity> findByPartyIdAndOwnerUserId(Long partyId, Long ownerUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from PartyBalanceEntity b where b.partyId = :partyId and b.ownerUserId = :ownerUserId")
    Optional<PartyBalanceEntity> lockByPartyIdAndOwnerUserId(@Param("partyId") long partyId, @Param("ownerUserId") long ownerUserId);

    List<PartyBalanceEntity> findByPartyIdIn(Collection<Long> partyIds);

    @Query("""
            select coalesce(sum(case when b.balanceMinor > 0 then b.balanceMinor else 0 end), 0),
                   coalesce(sum(case when b.balanceMinor < 0 then -b.balanceMinor else 0 end), 0)
            from PartyBalanceEntity b
            where b.ownerUserId = :ownerUserId
            """)
    Object[] summaryTotals(@Param("ownerUserId") long ownerUserId);

    // Used for Phase 4 "aging buckets" report. Returns rows: [balance_minor, last_activity_date].
    @Query(
            value = """
                    SELECT pb.balance_minor,
                           COALESCE(MAX(le.entry_date), DATE(p.created_at)) AS last_activity_date
                    FROM party_balances pb
                    JOIN parties p ON p.id = pb.party_id
                    LEFT JOIN ledger_entries le
                      ON le.owner_user_id = pb.owner_user_id
                     AND le.party_id = pb.party_id
                    WHERE pb.owner_user_id = :ownerUserId
                    GROUP BY pb.party_id, pb.balance_minor, p.created_at
                    """,
            nativeQuery = true
    )
    List<Object[]> balancesWithLastActivity(@Param("ownerUserId") long ownerUserId);

    long deleteByOwnerUserIdAndPartyId(Long ownerUserId, Long partyId);
}
