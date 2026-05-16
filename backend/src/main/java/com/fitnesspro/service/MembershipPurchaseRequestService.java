package com.fitnesspro.service;

import com.fitnesspro.dto.Dto.*;
import com.fitnesspro.entity.*;
import com.fitnesspro.entity.Enums.*;
import com.fitnesspro.exception.ApiException;
import com.fitnesspro.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MembershipPurchaseRequestService {
    private final MembershipPurchaseRequestRepository requests;
    private final ClientRepository clients;
    private final MembershipTypeRepository types;
    private final MembershipRepository memberships;
    private final MembershipService membershipService;
    private final NotificationService notifications;
    private final Mapper mapper;

    public MembershipPurchaseRequestService(MembershipPurchaseRequestRepository requests,
                                            ClientRepository clients,
                                            MembershipTypeRepository types,
                                            MembershipRepository memberships,
                                            MembershipService membershipService,
                                            NotificationService notifications,
                                            Mapper mapper) {
        this.requests = requests;
        this.clients = clients;
        this.types = types;
        this.memberships = memberships;
        this.membershipService = membershipService;
        this.notifications = notifications;
        this.mapper = mapper;
    }

    @Transactional
    public MembershipPurchaseRequestDto create(CreatePurchaseRequest body, User currentUser) {
        Client client = clients.findByUserEmail(currentUser.getEmail())
                .orElseThrow(() -> ApiException.forbidden("Профиль клиента не найден"));
        MembershipType type = types.findById(body.membershipTypeId())
                .orElseThrow(() -> ApiException.notFound("Тип абонемента не найден"));
        if (!type.isActive()) {
            throw ApiException.badRequest("Тип абонемента неактивен");
        }
        boolean alreadyPending = requests.findByClientOrderByCreatedAtDesc(client).stream()
                .anyMatch(r -> r.getStatus() == PurchaseRequestStatus.PENDING
                        && r.getMembershipType().getId().equals(type.getId()));
        if (alreadyPending) {
            throw ApiException.badRequest("Заявка на этот абонемент уже подана");
        }

        MembershipPurchaseRequest request = new MembershipPurchaseRequest();
        request.setClient(client);
        request.setMembershipType(type);
        request.setDesiredActivationDate(body.desiredActivationDate());
        request.setComment(body.comment());
        requests.save(request);

        notifications.notify(client.getUser(), "Заявка на абонемент отправлена",
                "Ваша заявка на абонемент \"" + type.getName() + "\" принята и ожидает подтверждения администратором.",
                NotificationType.MEMBERSHIP);
        return mapper.purchaseRequest(request);
    }

    public List<MembershipPurchaseRequestDto> all() {
        return requests.findAllByOrderByCreatedAtDesc().stream().map(mapper::purchaseRequest).toList();
    }

    public List<MembershipPurchaseRequestDto> pending() {
        return requests.findByStatusOrderByCreatedAtAsc(PurchaseRequestStatus.PENDING).stream()
                .map(mapper::purchaseRequest).toList();
    }

    public List<MembershipPurchaseRequestDto> my(User currentUser) {
        Client client = clients.findByUserEmail(currentUser.getEmail())
                .orElseThrow(() -> ApiException.forbidden("Профиль клиента не найден"));
        return requests.findByClientOrderByCreatedAtDesc(client).stream().map(mapper::purchaseRequest).toList();
    }

    @Transactional
    public MembershipPurchaseRequestDto approve(Long id, DecidePurchaseRequest body, User admin) {
        MembershipPurchaseRequest request = request(id);
        ensurePending(request);

        LocalDate activation = request.getDesiredActivationDate() == null
                ? LocalDate.now()
                : request.getDesiredActivationDate().isBefore(LocalDate.now())
                    ? LocalDate.now()
                    : request.getDesiredActivationDate();

        SellMembershipRequest sell = new SellMembershipRequest(
                request.getClient().getId(),
                request.getMembershipType().getId(),
                activation,
                PaymentMethod.CARD
        );
        // переиспользуем существующий поток продажи (включая запись Sale и проверку конфликтов)
        MembershipDto created = membershipService.sell(sell);

        request.setStatus(PurchaseRequestStatus.APPROVED);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecidedBy(admin);
        request.setDecisionComment(body == null ? null : body.comment());
        memberships.findById(created.id()).ifPresent(request::setCreatedMembership);

        notifications.notify(request.getClient().getUser(), "Заявка одобрена",
                "Заявка на абонемент \"" + request.getMembershipType().getName() + "\" одобрена. Абонемент активирован.",
                NotificationType.MEMBERSHIP);
        return mapper.purchaseRequest(request);
    }

    @Transactional
    public MembershipPurchaseRequestDto reject(Long id, DecidePurchaseRequest body, User admin) {
        MembershipPurchaseRequest request = request(id);
        ensurePending(request);
        request.setStatus(PurchaseRequestStatus.REJECTED);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecidedBy(admin);
        request.setDecisionComment(body == null ? null : body.comment());

        notifications.notify(request.getClient().getUser(), "Заявка отклонена",
                "Заявка на абонемент \"" + request.getMembershipType().getName() + "\" отклонена."
                        + (body != null && body.comment() != null && !body.comment().isBlank() ? " Причина: " + body.comment() : ""),
                NotificationType.MEMBERSHIP);
        return mapper.purchaseRequest(request);
    }

    @Transactional
    public MembershipPurchaseRequestDto cancel(Long id, User currentUser) {
        MembershipPurchaseRequest request = request(id);
        if (!request.getClient().getUser().getId().equals(currentUser.getId())) {
            throw ApiException.forbidden("Можно отменять только свои заявки");
        }
        ensurePending(request);
        request.setStatus(PurchaseRequestStatus.CANCELLED);
        request.setDecidedAt(LocalDateTime.now());
        return mapper.purchaseRequest(request);
    }

    private void ensurePending(MembershipPurchaseRequest request) {
        if (request.getStatus() != PurchaseRequestStatus.PENDING) {
            throw ApiException.badRequest("Заявка уже обработана");
        }
    }

    private MembershipPurchaseRequest request(Long id) {
        return requests.findById(id).orElseThrow(() -> ApiException.notFound("Заявка не найдена"));
    }
}
