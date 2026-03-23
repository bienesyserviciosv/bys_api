package app.bys.bys_api.repository;

import app.bys.bys_api.model.dto.MobilePaymentDto;
import app.bys.bys_api.model.dto.TransferPaymentDto;
import app.bys.bys_api.model.entity.Payment;
import app.bys.bys_api.model.enums.BankName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    @Query("SELECT COUNT(p) FROM Payment p  WHERE p.paymentStatus = 'ACCEPTED'")
    long countCompletedTransactions();

    @Query("SELECT COUNT(p) FROM Payment p  WHERE p.paymentStatus = 'PENDING'")
    long countPendingTransactions();

    @Query("""
                SELECT new app.bys.bys_api.model.dto.TransferPaymentDto(
                    p.id, p.bank, p.screenshot, p.idNumber, p.referenceNumber,
                    p.accountHolderName, p.paymentDate, p.offer.id,
                    p.amountInBolivars, p.paymentType, p.paymentStatus
                )
                FROM Payment p
                JOIN p.finalUser fu
                JOIN p.serviceProvider sp
                WHERE p.paymentType = 'TRANSFER'
                AND (:bank IS NULL OR p.bank = :bank)
                AND (:userIdList IS NULL OR fu.id IN (:userIdList))
                AND (:providerIdList IS NULL OR sp.id IN (:providerIdList))
            """)
    Page<TransferPaymentDto> findTransferPayments(
            @Param("bank") BankName bank,
            @Param("userIdList") List<Long> userIdList,
            @Param("providerIdList") List<Long> providerIdList,
            Pageable pageable);

    @Query("""
                SELECT new app.bys.bys_api.model.dto.MobilePaymentDto(
                    p.id, p.bank, p.screenshot, p.idNumber, p.referenceNumber,
                    p.paymentDate, p.phoneCode, p.phoneNumber, p.offer.id,
                    p.amountInBolivars, p.paymentType, p.paymentStatus
                )
                FROM Payment p
                JOIN p.finalUser fu
                JOIN p.serviceProvider sp
                WHERE p.paymentType = 'MOBILE'
                AND (:bank IS NULL OR p.bank = :bank)
                AND (:userIdList IS NULL OR fu.id IN (:userIdList))
                AND (:providerIdList IS NULL OR sp.id IN (:providerIdList))
            """)
    Page<MobilePaymentDto> findMobilePayments(@Param("bank") BankName bank,
                                              @Param("userIdList") List<Long> userIdList,
                                              @Param("providerIdList") List<Long> providerIdList,
                                              Pageable pageable);
}
