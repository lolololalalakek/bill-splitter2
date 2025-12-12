package uz.billsplitter2.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.billsplitter2.demo.entity.Guest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestRepository extends JpaRepository<Guest, UUID> {

    List<Guest> findAllByPartyId(UUID partyId);

    Optional<Guest> findByPartyIdAndName(UUID partyId, String name);
}
