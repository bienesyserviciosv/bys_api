package app.bys.bys_api.service;

import app.bys.bys_api.mapper.NotificationMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.NotificationDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.entity.Notification;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.model.enums.Province;
import app.bys.bys_api.repository.NotificationRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import app.bys.bys_api.service.specification.NotificationSpecification;
import app.bys.bys_api.utils.specification.SearchCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final NotificationMapper notificationMapper;

    public void notifyProviders(Long specializationId, Province address, ServiceRequest serviceRequest) {

        List<ServiceProvider> providers = serviceProviderRepository.findByAddressAndSpecializations_Id(address, specializationId);

        if (providers != null && !providers.isEmpty()) {
            List<Notification> notifications = providers.stream()
                    .map(provider -> Notification.builder()
                            .recipient(provider)
                            .message("Nueva solicitud disponible en tu zona")
                            .read(false)
                            .timestamp(LocalDateTime.now())
                            .serviceRequest(serviceRequest)
                            .build())
                    .collect(Collectors.toList());

            notificationRepository.saveAll(notifications);
            log.info("{} notification created", notifications.size());
        } else {
            log.warn("No providers found with this conditions");
        }
    }

    public PageDto<NotificationDto> getNotifications(Pageable pageable, String search, List<Long> providerIdList) {

        Specification<Notification> providerSpec =
                providerIdList != null ? NotificationSpecification.hasProvider(providerIdList)
                        : null;

        NotificationSpecification searchSpec =
                search != null ? new NotificationSpecification(
                        new SearchCriteria(
                                "name",
                                "s",
                                search
                        )
                )
                        : null;

        List<Specification<Notification>> specList = new ArrayList<>(Arrays.asList(
                providerSpec,
                searchSpec
        ));

        return PageMapper.pageToDto(notificationRepository.findAll(
                Specification.allOf(specList.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())),
                pageable).map(notificationMapper::toDto));

    }

}
