package com.fitnesspro.repository;

import com.fitnesspro.entity.Client;
import com.fitnesspro.entity.Enums.MembershipStatus;
import com.fitnesspro.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByClientOrderByActivationDateDesc(Client client);
    List<Membership> findByStatus(MembershipStatus status);
}
