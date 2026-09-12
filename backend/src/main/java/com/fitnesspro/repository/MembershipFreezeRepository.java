package com.fitnesspro.repository;

import com.fitnesspro.entity.Enums.MembershipFreezeStatus;
import com.fitnesspro.entity.Membership;
import com.fitnesspro.entity.MembershipFreeze;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipFreezeRepository extends JpaRepository<MembershipFreeze, Long> {
    List<MembershipFreeze> findByMembershipOrderByStartDateDesc(Membership membership);
    Optional<MembershipFreeze> findFirstByMembershipAndStatus(Membership membership, MembershipFreezeStatus status);
    List<MembershipFreeze> findByStatus(MembershipFreezeStatus status);

    default int totalFrozenDays(Membership membership) {
        return findByMembershipOrderByStartDateDesc(membership).stream()
                .filter(f -> f.getStatus() == MembershipFreezeStatus.FINISHED)
                .mapToInt(f -> (int) (f.getEndDate().toEpochDay() - f.getStartDate().toEpochDay()) + 1)
                .sum();
    }
}
