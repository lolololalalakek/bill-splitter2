package uz.billsplitter2.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.billsplitter2.demo.enums.WaiterRole;

@Entity
@Table(name = "waiters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Waiter extends BaseEntity {

    @Column(name = "keycloak_id", unique = true, nullable = false)
    private String keycloakId;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WaiterRole role = WaiterRole.WAITER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
