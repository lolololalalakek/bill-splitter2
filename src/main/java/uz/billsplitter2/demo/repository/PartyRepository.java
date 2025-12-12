package uz.billsplitter2.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.billsplitter2.demo.entity.Party;
import uz.billsplitter2.demo.enums.PartyStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartyRepository extends JpaRepository<Party, UUID> {

    List<Party> findAllByWaiterIdAndStatus(UUID waiterId, PartyStatus status);

    Optional<Party> findByTableIdAndStatus(UUID tableId, PartyStatus status);

    List<Party> findAllByStatus(PartyStatus status);

    List<Party> findAllByWaiterId(UUID waiterId);
}
