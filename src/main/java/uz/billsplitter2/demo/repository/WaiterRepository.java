package uz.billsplitter2.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.billsplitter2.demo.entity.Waiter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WaiterRepository extends JpaRepository<Waiter, UUID> {

    Optional<Waiter> findByKeycloakId(String keycloakId);

    Optional<Waiter> findByUsername(String username);

    List<Waiter> findAllByActive(boolean active);
}
