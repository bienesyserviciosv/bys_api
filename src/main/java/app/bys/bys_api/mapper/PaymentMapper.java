package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.MobilePaymentDto;
import app.bys.bys_api.model.dto.TransferPaymentDto;
import app.bys.bys_api.model.entity.Offer;
import app.bys.bys_api.model.entity.Payment;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Value;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public abstract class PaymentMapper {

    @Mapping(target = "offer", source = "offerId")
    public abstract Payment transferDtoToEntity(TransferPaymentDto transferPaymentDto);

    @Mapping(target = "offerId", source = "offer.id")
    public abstract TransferPaymentDto entityToTransferDto(Payment payment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updatePaymentFromTransferDto(TransferPaymentDto transferPaymentDto, @MappingTarget Payment payment);

    @Mapping(target = "offer", source = "offerId")
    public abstract Payment mobileDtoToEntity(MobilePaymentDto mobilePaymentDto);

    @Mapping(target = "offerId", source = "offer.id")
    public abstract MobilePaymentDto entityToMobileDto(Payment payment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updatePaymentFromMobileDto(MobilePaymentDto mobilePaymentDto, @MappingTarget Payment payment);

    protected Offer mapOffer(Long id) {
        return id == null ? null : Offer.builder().id(id).build();
    }

    @Value("${media.url}")
    public String mediaUrl;

    @AfterMapping
    public void addMediaUrlToMobilePaymentImage(@MappingTarget MobilePaymentDto mobilePaymentDto) {
        if (mobilePaymentDto.getScreenshot() != null) {
            mobilePaymentDto.setScreenshot(mediaUrl + mobilePaymentDto.getScreenshot());
        }
    }

    @AfterMapping
    public void addMediaUrlToTransferPaymentImage(@MappingTarget TransferPaymentDto transferPaymentDto) {
        if (transferPaymentDto.getScreenshot() != null) {
            transferPaymentDto.setScreenshot(mediaUrl + transferPaymentDto.getScreenshot());
        }
    }


}
