package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.ServiceRequestDto;
import app.bys.bys_api.model.dto.ServiceRequestInfo;
import app.bys.bys_api.model.dto.ServiceRequestWithPictureDto;
import app.bys.bys_api.model.entity.Payment;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.ServiceRequest;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {FinalUserMapper.class, OfferMapper.class, CommentMapper.class})
public abstract class ServiceRequestMapper {

    @Value("${media.url}")
    public String mediaUrl;

    @AfterMapping
    @BeanMapping(builder = @Builder(disableBuilder = true))
    public void addMediaUrlToImage(@MappingTarget ServiceRequestWithPictureDto requestDto) {
        requestDto.setPictureSet(
                requestDto.getPictureSet().stream()
                        .filter(Objects::nonNull)
                        .map(url -> url.startsWith(mediaUrl) ? url : mediaUrl + url)
                        .collect(Collectors.toSet()));
    }

    @BeforeMapping
    @BeanMapping(builder = @Builder(disableBuilder = true))
    public void pictureToUrl(@MappingTarget ServiceRequestWithPictureDto requestDto, ServiceRequest request) {
        requestDto.setPictureSet(request.getPictureSet().stream()
                .map(Picture::getUrl)
                .collect(Collectors.toSet()));
    }

    public abstract ServiceRequest dtoToEntity(ServiceRequestDto serviceRequestDto);

    @Mapping(target = "serviceProviderId", source = "serviceProvider.id")
    public abstract ServiceRequestDto entityToDto(ServiceRequest serviceRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateServiceRequestFromDto(ServiceRequestDto serviceRequestDto, @MappingTarget ServiceRequest serviceRequest);

    @Mapping(target = "serviceProviderId", source = "serviceProvider.id")
    @Mapping(target = "pictureSet", source = "pictureSet", qualifiedByName = "pictureToUrlSet")
    public abstract ServiceRequestWithPictureDto entityToDtoWithPicture(ServiceRequest serviceRequest);

    @Named("pictureToUrlSet")
    public Set<String> pictureToUrlSet(Set<Picture> pictures) {
        if (pictures == null) return Set.of();
        return pictures.stream()
                .map(Picture::getUrl)
                .filter(Objects::nonNull)
                .map(url -> url.startsWith(mediaUrl) ? url : mediaUrl + url)
                .collect(Collectors.toSet());
    }

    @Mapping(source = "finalUser.id", target = "userId")
    @Mapping(source = "requestStatus", target = "status")
    @Mapping(source = "comment", target = "commentDto")
    @Mapping(source = "offerSet", target = "offerDtoSet")
    @Mapping(source = "acceptedOffer.payment.id", target = "paymentId")
    @Mapping(source = "acceptedOffer.payment.paymentDate", target = "paymentDate")
    @Mapping(source = "acceptedOffer.payment.paymentType", target = "paymentType")
    @Mapping(source = "acceptedOffer.payment.amountInBolivars", target = "amountInBolivars")
    @Mapping(source = "acceptedOffer.price", target = "amountInUsd")
    @Mapping(target = "pictureSet", ignore = true)
    @Mapping(source = "specialization.specializationType", target = "specialization")
    @Mapping(target = "rating", expression = "java(serviceRequest.getComment() != null ? serviceRequest.getComment().getStarRating() : null)")
    public abstract ServiceRequestInfo entityToRequestInfo(ServiceRequest serviceRequest);

    @AfterMapping
    public void fillRequestInfoPaymentScreenshots(ServiceRequest source, @MappingTarget ServiceRequestInfo target) {
        String screenshot = null;
        if (source.getAcceptedOffer() != null && source.getAcceptedOffer().getPayment() != null) {
            Payment p = source.getAcceptedOffer().getPayment();
            screenshot = p.getScreenshot();
        }
        if (screenshot != null && !screenshot.isBlank()) {
            String url = screenshot.startsWith(mediaUrl) ? screenshot : mediaUrl + screenshot;
            target.setPictureSet(Set.of(url));
        } else {
            target.setPictureSet(Set.of());
        }
    }

}

