package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.SellMembershipRequest;
import com.fitnesspro.entity.Client;
import com.fitnesspro.entity.Enums.MembershipStatus;
import com.fitnesspro.entity.Enums.PaymentMethod;
import com.fitnesspro.entity.Membership;
import com.fitnesspro.entity.MembershipType;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.ClientRepository;
import com.fitnesspro.repository.MembershipFreezeRepository;
import com.fitnesspro.repository.MembershipRepository;
import com.fitnesspro.repository.MembershipTypeRepository;
import com.fitnesspro.repository.SaleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {
    @Mock private MembershipRepository memberships;
    @Mock private MembershipTypeRepository types;
    @Mock private ClientRepository clients;
    @Mock private SaleRepository sales;
    @Mock private MembershipFreezeRepository freezes;
    @Mock private NotificationService notifications;
    @Mock private Mapper mapper;

    @Test
    void sellRejectsInactiveMembershipType() {
        MembershipService service = service();
        Client client = new Client();
        MembershipType type = new MembershipType();
        type.setActive(false);
        when(memberships.findByStatus(MembershipStatus.ACTIVE)).thenReturn(List.of());
        when(clients.findById(1L)).thenReturn(Optional.of(client));
        when(types.findById(2L)).thenReturn(Optional.of(type));

        assertThatThrownBy(() -> service.sell(new SellMembershipRequest(1L, 2L, LocalDate.now(), PaymentMethod.CARD)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("неактивен");
    }

    @Test
    void chargeVisitMarksMembershipDepletedWhenLastVisitIsUsed() {
        MembershipService service = service();
        Membership membership = new Membership();
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setActivationDate(LocalDate.now().minusDays(1));
        membership.setExpirationDate(LocalDate.now().plusDays(1));
        membership.setRemainingVisits(1);

        service.chargeVisit(membership);

        assertThat(membership.getRemainingVisits()).isZero();
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.DEPLETED);
    }

    private MembershipService service() {
        return new MembershipService(memberships, types, clients, sales, freezes, notifications, mapper);
    }
}
