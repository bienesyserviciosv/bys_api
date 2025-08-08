package app.bys.bys_api.service;

import app.bys.bys_api.mapper.NotificationMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.NotificationDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.entity.Notification;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.repository.NotificationRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import app.bys.bys_api.service.specification.NotificationSpecification;
import app.bys.bys_api.utils.specification.SearchCriteria;
import lombok.RequiredArgsConstructor;
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
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final NotificationMapper notificationMapper;

    public String notifyProviders(Long specializationId, String address, ServiceRequest serviceRequest) {

        List<ServiceProvider> providers = serviceProviderRepository.findByAddressAndSpecializations_Id(address, specializationId);

        if (providers.isEmpty()) {
            return "There are no providers with this conditions";
        }
        for (ServiceProvider provider : providers) {
            Notification notification = Notification.builder()
                    .recipient(provider)
                    .message("Nueva solicitud disponible en tu zona")
                    .read(false)
                    .timestamp(LocalDateTime.now())
                    .serviceRequest(serviceRequest)
                    .build();
            notificationRepository.save(notification);
        }
            return "Notifications were sent";
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
