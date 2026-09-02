package org.azdev.barber_book.services;

import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.TenantRequest;
import org.azdev.barber_book.dtos.TenantResponse;
import org.azdev.barber_book.exception.BadRequestException;
import org.azdev.barber_book.exception.NotFoundException;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final SecurityUtils securityUtils;

    public Map<String, String> getPublicInfoBySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Barbearia não encontrada."));

        return Map.of("name", tenant.getName());
    }

    @Transactional
    public void updateMyBarbershop(TenantRequest request) {

        UUID tenantId = securityUtils.getCurrentTenantId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Barbearia não encontrada."));

        if (request.closingTime().isBefore(request.openingTime())) {
            throw new BadRequestException("O horário de fechamento não pode ser anterior ao de abertura.");
        }

        try {
            java.time.ZoneId.of(request.timezone());
        } catch (Exception e) {
            throw new BadRequestException("Fuso horário inválido. Exemplo válido: America/Sao_Paulo");
        }

        tenant.setName(request.name());
        tenant.setOpeningTime(request.openingTime());
        tenant.setClosingTime(request.closingTime());
        tenant.setTimezone(request.timezone());
        tenant.setSlotInterval(request.slotInterval());

        tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public TenantResponse getMyBarbershopSettings() {
        UUID tenantId = securityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Barbearia não encontrada."));

        return new TenantResponse(
                tenant.getName(),
                tenant.getOpeningTime(),
                tenant.getClosingTime(),
                tenant.getTimezone(),
                tenant.getSlotInterval()
        );
    }
}