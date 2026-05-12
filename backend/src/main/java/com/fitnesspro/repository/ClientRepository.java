package com.fitnesspro.repository;

import com.fitnesspro.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByUserEmail(String email);
    Optional<Client> findByRfidCard(String rfidCard);

    @Query("""
        select c from Client c
        where lower(c.user.fullName) like lower(concat('%', :q, '%'))
           or lower(c.user.email) like lower(concat('%', :q, '%'))
           or c.user.phone like concat('%', :q, '%')
           or c.rfidCard like concat('%', :q, '%')
        """)
    List<Client> search(String q);
}
