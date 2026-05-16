package com.fitnesspro.repository;

import com.fitnesspro.entity.Client;
import com.fitnesspro.entity.Enums.PurchaseRequestStatus;
import com.fitnesspro.entity.MembershipPurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipPurchaseRequestRepository extends JpaRepository<MembershipPurchaseRequest, Long> {
    List<MembershipPurchaseRequest> findByClientOrderByCreatedAtDesc(Client client);
    List<MembershipPurchaseRequest> findByStatusOrderByCreatedAtAsc(PurchaseRequestStatus status);
    List<MembershipPurchaseRequest> findAllByOrderByCreatedAtDesc();
}
