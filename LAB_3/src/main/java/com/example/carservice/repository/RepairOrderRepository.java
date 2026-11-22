package com.example.carservice.repository;

import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.RepairOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
public interface RepairOrderRepository extends JpaRepository<RepairOrder, Long> {

    @Query("""
            select distinct o
            from RepairOrder o
            left join fetch o.car
            left join fetch o.mechanic
            left join fetch o.works
            where o.car.vin = :vin
            order by o.createdAt
            """)
    List<RepairOrder> findDetailedByCarVin(@Param("vin") String vin);

    @Query("""
            select distinct o
            from RepairOrder o
            left join fetch o.car
            left join fetch o.mechanic
            left join fetch o.works
            where o.id = :id
            """)
    Optional<RepairOrder> findDetailedById(@Param("id") long id);

    @Query("""
            select distinct o
            from RepairOrder o
            left join fetch o.car
            left join fetch o.mechanic
            left join fetch o.works
            where o.mechanic = :mechanic
            order by o.createdAt
            """)
    List<RepairOrder> findDetailedByMechanic(@Param("mechanic") Mechanic mechanic);

    @Query("""
            select o
            from RepairOrder o
            join fetch o.car
            where o.car.vin like concat(:prefix, '%')
            order by o.createdAt
            """)
    List<RepairOrder> findAllWithCarOnly(@Param("prefix") String prefix);

    @EntityGraph(attributePaths = {"car", "mechanic", "works"})
    @Query("""
            select distinct o
            from RepairOrder o
            left join fetch o.car
            left join fetch o.mechanic
            left join fetch o.works
            where o.car.vin like concat(:prefix, '%')
            order by o.createdAt
            """)
    List<RepairOrder> findDetailedByCarVinPrefix(String prefix);

    @Query(value = """
            select ro.*
            from repair_orders ro
            where ro.mechanic_id = :mechanicId
              and ro.status not in ('" + COMPLETED_STATUS + "', '" + CANCELLED_STATUS + "')
            order by ro.created_at
            """, nativeQuery = true)
    List<RepairOrder> findActiveByMechanicNative(long mechanicId);

    @Query(value = """
            select ro.*
            from repair_orders ro
            where ro.status = :status
              and ro.created_at between :from and :to
            order by ro.created_at
            """, nativeQuery = true)
    List<RepairOrder> findByStatusBetweenNative(@Param("status") String status,
                                                @Param("from") Instant from,
                                                @Param("to") Instant to);
}
