package uz.billsplitter2.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.billsplitter2.demo.entity.RestaurantTable;
import uz.billsplitter2.demo.enums.TableStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, UUID> {

    Optional<RestaurantTable> findByTableNumber(String tableNumber);

    List<RestaurantTable> findAllByStatus(TableStatus status);
}
