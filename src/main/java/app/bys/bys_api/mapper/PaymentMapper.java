package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.MobilePaymentDto;
import app.bys.bys_api.model.dto.TransferPaymentDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Payment;
import app.bys.bys_api.model.entity.ServiceProvider;
import org.mapstruct.*;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public abstract class PaymentMapper {

    @Mapping(target = "finalUser", source = "finalUserId")
    @Mapping(target = "serviceProvider", source = "serviceProviderId")
    public abstract Payment transferDtoToEntity(TransferPaymentDto transferPaymentDto);

    @Mapping(target = "finalUserId", source = "finalUser.id")
    @Mapping(target = "serviceProviderId", source = "serviceProvider.id")
    public abstract TransferPaymentDto entityToTransferDto(Payment payment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updatePaymentFromTransferDto(TransferPaymentDto transferPaymentDto, @MappingTarget Payment payment);


    @Mapping(target = "finalUser", source = "finalUserId")
    @Mapping(target = "serviceProvider", source = "serviceProviderId")
    public abstract Payment mobileDtoToEntity(MobilePaymentDto mobilePaymentDto);

    @Mapping(target = "finalUserId", source = "finalUser.id")
    @Mapping(target = "serviceProviderId", source = "serviceProvider.id")
    public abstract MobilePaymentDto entityToMobileDto(Payment payment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updatePaymentFromMobileDto(MobilePaymentDto mobilePaymentDto, @MappingTarget Payment payment);

    protected FinalUser mapFinalUser(Long id) {
        return id == null ? null : FinalUser.builder().id(id).build();
    }

    protected ServiceProvider mapServiceProvider(Long id) {
        return id == null ? null : ServiceProvider.builder().id(id).build();
    }

}
