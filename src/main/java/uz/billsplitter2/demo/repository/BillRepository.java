package uz.billsplitter2.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.billsplitter2.demo.entity.Bill;
import uz.billsplitter2.demo.enums.BillStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<Bill, UUID> {

    List<Bill> findAllByPartyId(UUID partyId);

    Optional<Bill> findByPartyIdAndStatus(UUID partyId, BillStatus status);

    Optional<Bill> findByBillNumber(String billNumber);

    List<Bill> findAllByStatus(BillStatus status);

    @Query("SELECT b FROM Bill b WHERE b.status = :status " +
           "AND (:startDate IS NULL OR b.closedAt >= :startDate) " +
           "AND (:endDate IS NULL OR b.closedAt <= :endDate) " +
           "AND (:waiterId IS NULL OR b.party.waiter.id = :waiterId) " +
           "ORDER BY b.closedAt DESC")
    List<Bill> findClosedBillsWithFilters(
        @Param("status") BillStatus status,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate,
        @Param("waiterId") UUID waiterId
    );
}
