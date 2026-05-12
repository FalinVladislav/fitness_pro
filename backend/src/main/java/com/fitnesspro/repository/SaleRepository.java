package com.fitnesspro.repository;

import com.fitnesspro.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByPaymentDateBetween(LocalDateTime from, LocalDateTime to);

    @Query("select coalesce(sum(s.amount), 0) from Sale s where s.paymentDate between :from and :to")
    BigDecimal revenue(LocalDateTime from, LocalDateTime to);
}
