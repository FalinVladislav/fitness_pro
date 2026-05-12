package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.entity.*;
import com.fitnesspro.entity.Enums.*;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class MembershipService {
    private final MembershipRepository memberships;
    private final MembershipTypeRepository types;
    private final ClientRepository clients;
    private final SaleRepository sales;
    private final Mapper mapper;

    public MembershipService(MembershipRepository memberships, MembershipTypeRepository types,
                             ClientRepository clients, SaleRepository sales, Mapper mapper) {
        this.memberships = memberships;
        this.types = types;
        this.clients = clients;
        this.sales = sales;
        this.mapper = mapper;
    }

    public List<MembershipDto> all() {
        refreshExpired();
        return memberships.findAll().stream().map(mapper::membership).toList();
    }

    public List<MembershipDto> byClient(Long clientId) {
        refreshExpired();
        Client client = client(clientId);
        return memberships.findByClientOrderByActivationDateDesc(client).stream().map(mapper::membership).toList();
    }

    public List<MembershipDto> byCurrentClient(User user) {
        Client client = clients.findByUserEmail(user.getEmail()).orElseThrow(() -> ApiException.forbidden("Профиль клиента не найден"));
        return memberships.findByClientOrderByActivationDateDesc(client).stream().map(mapper::membership).toList();
    }

    @Transactional
    public MembershipDto sell(SellMembershipRequest request) {
        Client client = client(request.clientId());
        MembershipType type = types.findById(request.membershipTypeId()).orElseThrow(() -> ApiException.notFound("Тип абонемента не найден"));
        if (!type.isActive()) {
            throw ApiException.badRequest("Тип абонемента неактивен");
        }
        LocalDate activation = request.activationDate() == null ? LocalDate.now() : request.activationDate();
        Membership membership = new Membership();
        membership.setClient(client);
        membership.setMembershipType(type);
        membership.setActivationDate(activation);
        membership.setExpirationDate(activation.plusDays(type.getDurationDays()));
        membership.setRemainingVisits(type.getVisitCount());
        memberships.save(membership);

        Sale sale = new Sale();
        sale.setClient(client);
        sale.setMembership(membership);
        sale.setAmount(type.getPrice());
        sale.setPaymentMethod(request.paymentMethod() == null ? PaymentMethod.CARD : request.paymentMethod());
        sales.save(sale);
        return mapper.membership(membership);
    }

    @Transactional
    public MembershipDto renew(Long id) {
        Membership current = membership(id);
        SellMembershipRequest request = new SellMembershipRequest(current.getClient().getId(), current.getMembershipType().getId(),
                LocalDate.now().isAfter(current.getExpirationDate()) ? LocalDate.now() : current.getExpirationDate().plusDays(1),
                PaymentMethod.CARD);
        return sell(request);
    }

    public MembershipDto status(Long id) {
        refresh(membership(id));
        return mapper.membership(membership(id));
    }

    @Transactional
    public Membership activeFor(Client client) {
        refreshExpired();
        return memberships.findByClientOrderByActivationDateDesc(client).stream()
                .filter(this::usable)
                .max(Comparator.comparing(Membership::getExpirationDate))
                .orElseThrow(() -> ApiException.badRequest("У клиента нет активного абонемента"));
    }

    @Transactional
    public void chargeVisit(Membership membership) {
        refresh(membership);
        if (!usable(membership)) {
            throw ApiException.badRequest("Абонемент недействителен");
        }
        if (membership.getRemainingVisits() != null) {
            if (membership.getRemainingVisits() <= 0) {
                membership.setStatus(MembershipStatus.DEPLETED);
                throw ApiException.badRequest("Посещения по абонементу исчерпаны");
            }
            membership.setRemainingVisits(membership.getRemainingVisits() - 1);
            if (membership.getRemainingVisits() == 0) {
                membership.setStatus(MembershipStatus.DEPLETED);
            }
        }
    }

    private boolean usable(Membership m) {
        refresh(m);
        return m.getStatus() == MembershipStatus.ACTIVE
                && !LocalDate.now().isBefore(m.getActivationDate())
                && !LocalDate.now().isAfter(m.getExpirationDate())
                && (m.getRemainingVisits() == null || m.getRemainingVisits() > 0);
    }

    @Transactional
    public void refreshExpired() {
        memberships.findByStatus(MembershipStatus.ACTIVE).forEach(this::refresh);
    }

    private void refresh(Membership m) {
        if (m.getStatus() == MembershipStatus.ACTIVE && LocalDate.now().isAfter(m.getExpirationDate())) {
            m.setStatus(MembershipStatus.EXPIRED);
        }
        if (m.getStatus() == MembershipStatus.ACTIVE && m.getRemainingVisits() != null && m.getRemainingVisits() <= 0) {
            m.setStatus(MembershipStatus.DEPLETED);
        }
    }

    private Membership membership(Long id) {
        return memberships.findById(id).orElseThrow(() -> ApiException.notFound("Абонемент не найден"));
    }

    private Client client(Long id) {
        return clients.findById(id).orElseThrow(() -> ApiException.notFound("Клиент не найден"));
    }
}
