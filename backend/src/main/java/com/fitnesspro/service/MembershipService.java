package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.entity.*;
import com.fitnesspro.entity.Enums.*;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class MembershipService {
    private final MembershipRepository memberships;
    private final MembershipTypeRepository types;
    private final ClientRepository clients;
    private final SaleRepository sales;
    private final MembershipFreezeRepository freezes;
    private final NotificationService notifications;
    private final Mapper mapper;

    public MembershipService(MembershipRepository memberships, MembershipTypeRepository types,
                             ClientRepository clients, SaleRepository sales,
                             MembershipFreezeRepository freezes, NotificationService notifications, Mapper mapper) {
        this.memberships = memberships;
        this.types = types;
        this.clients = clients;
        this.sales = sales;
        this.freezes = freezes;
        this.notifications = notifications;
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
        refreshExpired();
        Client client = client(request.clientId());
        MembershipType type = types.findById(request.membershipTypeId()).orElseThrow(() -> ApiException.notFound("Тип абонемента не найден"));
        if (!type.isActive()) {
            throw ApiException.badRequest("Тип абонемента неактивен");
        }
        LocalDate activation = request.activationDate() == null ? LocalDate.now() : request.activationDate();
        LocalDate expiration = activation.plusDays(type.getDurationDays());
        boolean overlaps = memberships.findByClientOrderByActivationDateDesc(client).stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE || m.getStatus() == MembershipStatus.FROZEN)
                .anyMatch(m -> !expiration.isBefore(m.getActivationDate()) && !activation.isAfter(m.getExpirationDate()));
        if (overlaps) {
            throw ApiException.badRequest("Нельзя продать абонемент: срок действия пересекается с существующим абонементом клиента");
        }
        Membership membership = new Membership();
        membership.setClient(client);
        membership.setMembershipType(type);
        membership.setActivationDate(activation);
        membership.setExpirationDate(expiration);
        membership.setRemainingVisits(type.getVisitCount());
        memberships.save(membership);

        Sale sale = new Sale();
        sale.setClient(client);
        sale.setMembership(membership);
        sale.setAmount(type.getPrice());
        sale.setPaymentMethod(request.paymentMethod() == null ? PaymentMethod.CARD : request.paymentMethod());
        sales.save(sale);

        notifications.notify(client.getUser(), "Абонемент оформлен",
                "Вы оформили абонемент \"" + type.getName() + "\" до " + expiration + ".",
                NotificationType.MEMBERSHIP);
        return mapper.membership(membership);
    }

    @Transactional
    public MembershipDto buyForCurrentClient(CreatePurchaseRequest request, User currentUser) {
        Client client = clients.findByUserEmail(currentUser.getEmail())
                .orElseThrow(() -> ApiException.forbidden("Профиль клиента не найден"));
        LocalDate activation = request.desiredActivationDate() == null || request.desiredActivationDate().isBefore(LocalDate.now())
                ? LocalDate.now()
                : request.desiredActivationDate();
        MembershipDto membership = sell(new SellMembershipRequest(client.getId(), request.membershipTypeId(), activation, PaymentMethod.ONLINE));
        notifications.notify(client.getUser(), "Покупка абонемента подтверждена",
                "Абонемент \"" + membership.typeName() + "\" оплачен онлайн и активирован до " + membership.expirationDate() + ".",
                NotificationType.MEMBERSHIP);
        return membership;
    }

    @Transactional
    public MembershipDto renew(Long id) {
        Membership current = membership(id);
        SellMembershipRequest request = new SellMembershipRequest(current.getClient().getId(), current.getMembershipType().getId(),
                LocalDate.now().isAfter(current.getExpirationDate()) ? LocalDate.now() : current.getExpirationDate().plusDays(1),
                PaymentMethod.CARD);
        MembershipDto created = sell(request);
        notifications.notify(current.getClient().getUser(), "Абонемент продлен",
                "Абонемент \"" + current.getMembershipType().getName() + "\" продлен. Новый срок: до " + created.expirationDate() + ".",
                NotificationType.MEMBERSHIP);
        return created;
    }

    public MembershipDto status(Long id) {
        refresh(membership(id));
        return mapper.membership(membership(id));
    }

    @Transactional
    public MembershipDto cancel(Long id) {
        Membership m = membership(id);
        if (m.getStatus() == MembershipStatus.CANCELLED) {
            throw ApiException.badRequest("Абонемент уже отменен");
        }
        m.setStatus(MembershipStatus.CANCELLED);
        notifications.notify(m.getClient().getUser(), "Абонемент отменен",
                "Ваш абонемент \"" + m.getMembershipType().getName() + "\" отменен.",
                NotificationType.MEMBERSHIP);
        return mapper.membership(m);
    }

    @Transactional
    public MembershipFreezeDto freeze(Long id, FreezeMembershipRequest request) {
        Membership m = membership(id);
        MembershipType type = m.getMembershipType();
        if (!type.isFreezeAllowed()) {
            throw ApiException.badRequest("Заморозка для этого тарифа не разрешена");
        }
        if (m.getStatus() != MembershipStatus.ACTIVE) {
            throw ApiException.badRequest("Заморозить можно только активный абонемент");
        }
        if (request.startDate().isAfter(request.endDate())) {
            throw ApiException.badRequest("Дата начала позже даты окончания");
        }
        if (request.startDate().isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Заморозка не может начинаться в прошлом");
        }
        if (request.endDate().isAfter(m.getExpirationDate())) {
            throw ApiException.badRequest("Заморозка выходит за срок действия абонемента");
        }
        int requested = (int) ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        int alreadyUsed = freezes.totalFrozenDays(m);
        if (type.getMaxFreezeDays() != null && type.getMaxFreezeDays() > 0
                && alreadyUsed + requested > type.getMaxFreezeDays()) {
            throw ApiException.badRequest("Превышен лимит дней заморозки: " + type.getMaxFreezeDays());
        }

        MembershipFreeze freeze = new MembershipFreeze();
        freeze.setMembership(m);
        freeze.setStartDate(request.startDate());
        freeze.setEndDate(request.endDate());
        freeze.setReason(request.reason());
        freeze.setStatus(MembershipFreezeStatus.ACTIVE);
        freezes.save(freeze);

        m.setStatus(MembershipStatus.FROZEN);
        m.setExpirationDate(m.getExpirationDate().plusDays(requested));

        notifications.notify(m.getClient().getUser(), "Абонемент заморожен",
                "Абонемент \"" + type.getName() + "\" заморожен с " + request.startDate() + " по " + request.endDate() + ".",
                NotificationType.MEMBERSHIP);
        return mapper.freeze(freeze);
    }

    @Transactional
    public MembershipFreezeDto unfreeze(Long id) {
        Membership m = membership(id);
        MembershipFreeze active = freezes.findFirstByMembershipAndStatus(m, MembershipFreezeStatus.ACTIVE)
                .orElseThrow(() -> ApiException.badRequest("У абонемента нет активной заморозки"));
        LocalDate today = LocalDate.now();
        if (today.isBefore(active.getEndDate())) {
            // заморозка завершается досрочно — возвращаем неиспользованные дни
            int planned = (int) ChronoUnit.DAYS.between(active.getStartDate(), active.getEndDate()) + 1;
            int actual = (int) ChronoUnit.DAYS.between(active.getStartDate(),
                    today.isBefore(active.getStartDate()) ? active.getStartDate() : today) + 1;
            int diff = planned - actual;
            if (diff > 0) {
                m.setExpirationDate(m.getExpirationDate().minusDays(diff));
            }
            active.setEndDate(today.isBefore(active.getStartDate()) ? active.getStartDate() : today);
        }
        active.setStatus(MembershipFreezeStatus.FINISHED);
        m.setStatus(MembershipStatus.ACTIVE);
        refresh(m);
        notifications.notify(m.getClient().getUser(), "Абонемент разморожен",
                "Абонемент \"" + m.getMembershipType().getName() + "\" снова активен. Срок действия: до " + m.getExpirationDate() + ".",
                NotificationType.MEMBERSHIP);
        return mapper.freeze(active);
    }

    public List<MembershipFreezeDto> freezesOf(Long membershipId) {
        Membership m = membership(membershipId);
        return freezes.findByMembershipOrderByStartDateDesc(m).stream().map(mapper::freeze).toList();
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

    @Transactional
    public void restoreVisit(Membership membership) {
        if (membership == null || membership.getRemainingVisits() == null) {
            return;
        }
        membership.setRemainingVisits(membership.getRemainingVisits() + 1);
        if (membership.getStatus() == MembershipStatus.DEPLETED
                && !LocalDate.now().isBefore(membership.getActivationDate())
                && !LocalDate.now().isAfter(membership.getExpirationDate())) {
            membership.setStatus(MembershipStatus.ACTIVE);
        }
    }

    /**
     * Возвращает абонемент клиента если он есть и пригоден, либо null. В отличие от activeFor не выбрасывает исключение —
     * нужен для разовых платных визитов без абонемента.
     */
    @Transactional
    public Membership findUsable(Client client) {
        refreshExpired();
        return memberships.findByClientOrderByActivationDateDesc(client).stream()
                .filter(this::usable)
                .max(Comparator.comparing(Membership::getExpirationDate))
                .orElse(null);
    }

    public boolean isOwner(Long membershipId, User user) {
        return memberships.findById(membershipId)
                .map(m -> m.getClient().getUser().getId().equals(user.getId()))
                .orElse(false);
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
            notifications.notify(m.getClient().getUser(), "Срок абонемента истёк",
                    "Абонемент \"" + m.getMembershipType().getName() + "\" завершён " + m.getExpirationDate() + ".",
                    NotificationType.MEMBERSHIP);
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
