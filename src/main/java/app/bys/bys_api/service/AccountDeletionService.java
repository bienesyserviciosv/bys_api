package app.bys.bys_api.service;

import app.bys.bys_api.error.ConflictException;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.MediaRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import app.bys.bys_api.model.enums.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Borrado completo de cuentas (cliente, proveedor, administrador) desde el panel
 * del super-admin (2026-10-05).
 *
 * Por que existe: la base de datos NO tiene ON DELETE CASCADE en ninguna clave
 * foranea, y las entidades JPA solo tienen cascade parcial. Borrar con
 * repository.delete(...) un usuario que ya tiene pagos, comentarios u ofertas
 * fallaba con una violacion de clave foranea (500). Este servicio borra primero,
 * en orden, todo lo que depende del usuario y recien al final el usuario.
 *
 * Decision de Lachy (2026-10-06, para coincidir con la politica de privacidad):
 * al eliminar una cuenta se borran los datos personales, pero se CONSERVAN durante
 * RETENTION_YEARS anios los registros transaccionales (pagos y las ofertas/solicitudes
 * vinculadas a ellos), sin datos que identifiquen a la persona. Si la cuenta no tiene
 * pagos, se borra TODO (como antes). Cuando la cuenta tiene pagos, la fila del usuario
 * queda como "tumba" anonimizada (rol ROLE_DELETED, sin credenciales, estado BLOCKED),
 * invisible en los listados del panel, y el barrido semanal
 * purgeExpiredRetainedRecords() la borra definitivamente al vencer el plazo.
 *
 * Todo ocurre en una sola transaccion: si algo falla, no se borra nada.
 * Los archivos de imagen (S3) se borran recien al final, una vez que todo el SQL
 * ya paso bien, y un fallo al borrar un archivo no cancela el borrado de la cuenta.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_DELETED = "ROLE_DELETED";

    /** Plazo de conservacion de registros transaccionales; debe coincidir con la politica de privacidad. */
    public static final int RETENTION_YEARS = 2;

    private final EntityManager entityManager;
    private final FinalUserRepository finalUserRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final MediaRepository mediaRepository;
    private final RoleService roleService;

    // ------------------------------------------------------------------
    // Entradas publicas
    // ------------------------------------------------------------------

    /** Elimina un cliente (ROLE_USER). No permite borrar administradores. */
    @Transactional
    public void deleteClient(Long id) {
        FinalUser user = findUser(id);
        if (hasRole(user, ROLE_SUPER_ADMIN) || hasRole(user, ROLE_ADMIN)) {
            throw new ConflictException("Este usuario es administrador, no cliente. Eliminalo desde la seccion Administradores.");
        }
        purgeFinalUser(user);
    }

    /** Elimina un administrador (ROLE_ADMIN). Nunca permite borrar al super-admin. */
    @Transactional
    public void deleteAdmin(Long id) {
        FinalUser user = findUser(id);
        if (hasRole(user, ROLE_SUPER_ADMIN)) {
            throw new ConflictException("No se puede eliminar al super administrador.");
        }
        if (!hasRole(user, ROLE_ADMIN)) {
            throw new ConflictException("Este usuario no es administrador.");
        }
        purgeFinalUser(user);
    }

    /**
     * Auto-eliminacion de cuenta desde la app (boton "Eliminar cuenta" del perfil) para
     * clientes. Usa el mismo borrado en cadena que el panel admin: antes se borraba solo
     * la fila del usuario y fallaba con error 500 si ya tenia solicitudes, pagos, etc.
     * Nunca permite borrar al super-admin.
     */
    @Transactional
    public void deleteOwnFinalUser(String email) {
        FinalUser user = finalUserRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Final user with email: " + email + " not found"));
        if (hasRole(user, ROLE_SUPER_ADMIN)) {
            throw new ConflictException("No se puede eliminar al super administrador.");
        }
        log.info("Usuario {} solicito eliminar su propia cuenta", email);
        purgeFinalUser(user);
    }

    /** Auto-eliminacion de cuenta desde la app para proveedores. */
    @Transactional
    public void deleteOwnProvider(String email) {
        ServiceProvider provider = serviceProviderRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Service provider with email " + email + " not found"));
        log.info("Proveedor {} solicito eliminar su propia cuenta", email);
        deleteProvider(provider.getId());
    }

    /**
     * Elimina un proveedor. Si tiene pagos, se anonimiza y se conservan sus registros
     * transaccionales; si no, se borra todo lo relacionado con el.
     */
    @Transactional
    public void deleteProvider(Long id) {
        ServiceProvider provider = findProvider(id);
        List<Long> paymentIds = ids("select p.id from Payment p where p.serviceProvider.id = :v", id);
        if (paymentIds.isEmpty()) {
            hardDeleteProvider(provider);
        } else {
            anonymizeProvider(provider, paymentIds);
        }
    }

    /** Borrado total del proveedor (sin pagos que conservar, o al vencer el plazo de retencion). */
    private void hardDeleteProvider(ServiceProvider provider) {
        Long id = provider.getId();
        String email = provider.getEmail();
        List<String> mediaUrls = new ArrayList<>();
        try {
            // Solicitudes donde este proveedor fue el elegido (o su oferta fue la aceptada):
            // se borran completas para no dejar solicitudes en un estado inconsistente.
            List<Long> requestIds = ids("select r.id from ServiceRequest r where r.serviceProvider.id = :v "
                    + "or r.acceptedOffer.id in (select o.id from Offer o where o.provider.id = :v)", id);
            deleteRequests(requestIds, mediaUrls);

            // Ofertas del proveedor en solicitudes abiertas de otros clientes.
            deleteOffers(ids("select o.id from Offer o where o.provider.id = :v", id), mediaUrls);

            deletePayments(ids("select p.id from Payment p where p.serviceProvider.id = :v", id), mediaUrls);
            exec("delete from Comment c where c.provider.id = :v", id);
            exec("delete from Notification n where n.serviceProvider.id = :v", id);
            deletePictures("p.serviceProvider.id = :v", id, mediaUrls);

            entityManager.flush();
            entityManager.clear();

            // Se vuelve a leer limpio: ya no tiene hijos, el delete de JPA solo
            // limpia las tablas de union (roles y especializaciones).
            serviceProviderRepository.delete(findProvider(id));
            entityManager.flush();
        } catch (DataIntegrityViolationException e) {
            log.error("No se pudo eliminar el proveedor {}: {}", id, e.getMostSpecificCause().getMessage());
            throw new ConflictException("No se pudo eliminar el proveedor: quedaron datos relacionados que no se pudieron limpiar.");
        }
        deleteMedia(mediaUrls);
        log.info("Proveedor {} ({}) eliminado definitivamente", id, email);
    }

    /**
     * Anonimiza al proveedor conservando sus pagos (y las ofertas/solicitudes vinculadas).
     * Los datos del pagador y de la solicitud pertenecen al cliente, que sigue activo,
     * por eso aqui no se tocan; solo se limpia lo que pertenece al proveedor.
     */
    private void anonymizeProvider(ServiceProvider provider, List<Long> paymentIds) {
        Long id = provider.getId();
        String email = provider.getEmail();
        List<String> mediaUrls = new ArrayList<>();
        try {
            List<Long> keepOfferIds = ids("select p.offer.id from Payment p where p.id in :v and p.offer is not null", paymentIds);
            List<Long> keepRequestIds = keepOfferIds.isEmpty()
                    ? new ArrayList<>()
                    : ids("select o.serviceRequest.id from Offer o where o.id in :v", keepOfferIds);

            // Solicitudes donde fue el elegido, salvo las que tienen pago conservado.
            List<Long> requestIds = ids("select r.id from ServiceRequest r where r.serviceProvider.id = :v "
                    + "or r.acceptedOffer.id in (select o.id from Offer o where o.provider.id = :v)", id);
            requestIds.removeAll(keepRequestIds);
            deleteRequests(requestIds, mediaUrls);

            // Ofertas sin pago asociado.
            List<Long> offerIds = ids("select o.id from Offer o where o.provider.id = :v", id);
            offerIds.removeAll(keepOfferIds);
            deleteOffers(offerIds, mediaUrls);

            // En las ofertas conservadas se borra el texto libre escrito por el proveedor.
            if (!keepOfferIds.isEmpty()) {
                exec("update Offer o set o.description = null where o.id in :v", keepOfferIds);
            }

            exec("delete from Comment c where c.provider.id = :v", id);
            exec("delete from Notification n where n.serviceProvider.id = :v", id);
            deletePictures("p.serviceProvider.id = :v", id, mediaUrls);

            entityManager.flush();
            entityManager.clear();

            ServiceProvider fresh = findProvider(id);
            fresh.setName("Proveedor eliminado");
            fresh.setEmail("deleted-p" + id + "@deleted.invalid");
            fresh.setPhoneNumber("deleted-p" + id); // columna obligatoria y unica
            fresh.setPassword(UUID.randomUUID().toString()); // no es un hash valido: nadie puede iniciar sesion
            fresh.setExperience(null);
            fresh.setLatitude(null);
            fresh.setLongitude(null);
            fresh.setAddress(null);
            fresh.setProfilePicture(null);
            fresh.setFcmToken(null);
            fresh.setAdminVerified(false);
            fresh.setEmailVerified(false);
            fresh.setPhoneVerified(false);
            fresh.setStatus(UserStatus.BLOCKED);
            fresh.setSpecializations(new HashSet<>());
            fresh.setRoles(new HashSet<>(Set.of(roleService.getOrCreateRole(ROLE_DELETED))));
            serviceProviderRepository.save(fresh);
            entityManager.flush();
        } catch (DataIntegrityViolationException e) {
            log.error("No se pudo anonimizar el proveedor {}: {}", id, e.getMostSpecificCause().getMessage());
            throw new ConflictException("No se pudo eliminar el proveedor: quedaron datos relacionados que no se pudieron limpiar.");
        }
        deleteMedia(mediaUrls);
        log.info("Proveedor {} ({}) eliminado: datos personales borrados, {} pago(s) conservado(s) por {} anios",
                id, email, paymentIds.size(), RETENTION_YEARS);
    }

    // ------------------------------------------------------------------
    // Borrado de un FinalUser (cliente o administrador)
    // ------------------------------------------------------------------

    /** Si el usuario tiene pagos se anonimiza (conserva registros transaccionales); si no, se borra todo. */
    private void purgeFinalUser(FinalUser user) {
        List<Long> paymentIds = ids("select p.id from Payment p where p.finalUser.id = :v", user.getId());
        if (paymentIds.isEmpty()) {
            hardDeleteFinalUser(user);
        } else {
            anonymizeFinalUser(user, paymentIds);
        }
    }

    private void hardDeleteFinalUser(FinalUser user) {
        Long id = user.getId();
        String email = user.getEmail();
        List<String> mediaUrls = new ArrayList<>();
        try {
            deleteRequests(ids("select r.id from ServiceRequest r where r.finalUser.id = :v", id), mediaUrls);
            deleteOffers(ids("select o.id from Offer o where o.finalUser.id = :v", id), mediaUrls);
            deletePayments(ids("select p.id from Payment p where p.finalUser.id = :v", id), mediaUrls);
            exec("delete from Comment c where c.author.id = :v", id);
            exec("delete from Notification n where n.finalUser.id = :v", id);
            deletePictures("p.finalUser.id = :v", id, mediaUrls);

            entityManager.flush();
            entityManager.clear();

            finalUserRepository.delete(findUser(id));
            entityManager.flush();
        } catch (DataIntegrityViolationException e) {
            log.error("No se pudo eliminar el usuario {}: {}", id, e.getMostSpecificCause().getMessage());
            throw new ConflictException("No se pudo eliminar el usuario: quedaron datos relacionados que no se pudieron limpiar.");
        }
        deleteMedia(mediaUrls);
        log.info("Usuario {} ({}) eliminado definitivamente", id, email);
    }

    /**
     * Anonimiza al usuario conservando sus pagos y las ofertas/solicitudes vinculadas.
     * En lo conservado se borran los datos personales del usuario (telefono, cedula y
     * titular del pago, comprobante, descripcion y ubicacion de la solicitud, fotos).
     */
    private void anonymizeFinalUser(FinalUser user, List<Long> paymentIds) {
        Long id = user.getId();
        String email = user.getEmail();
        List<String> mediaUrls = new ArrayList<>();
        try {
            List<Long> keepOfferIds = ids("select p.offer.id from Payment p where p.id in :v and p.offer is not null", paymentIds);
            List<Long> keepRequestIds = keepOfferIds.isEmpty()
                    ? new ArrayList<>()
                    : ids("select o.serviceRequest.id from Offer o where o.id in :v", keepOfferIds);

            // Todo lo que NO tiene pago asociado se borra completo.
            List<Long> requestIds = ids("select r.id from ServiceRequest r where r.finalUser.id = :v", id);
            requestIds.removeAll(keepRequestIds);
            deleteRequests(requestIds, mediaUrls);

            List<Long> offerIds = ids("select o.id from Offer o where o.finalUser.id = :v", id);
            offerIds.removeAll(keepOfferIds);
            deleteOffers(offerIds, mediaUrls);

            // Pagos conservados: se borra el comprobante (foto) y los datos personales del pagador.
            deletePictures("p.payment.id in :v", paymentIds, mediaUrls);
            exec("update Payment p set p.screenshot = null, p.phoneNumber = null, p.idNumber = null, "
                    + "p.accountHolderName = null where p.id in :v", paymentIds);

            // Solicitudes conservadas: se borra el texto libre, la ubicacion exacta y las fotos.
            if (!keepRequestIds.isEmpty()) {
                exec("update ServiceRequest r set r.description = null, r.latitude = null, r.longitude = null "
                        + "where r.id in :v", keepRequestIds);
                deletePictures("p.serviceRequest.id in :v", keepRequestIds, mediaUrls);
            }

            exec("delete from Comment c where c.author.id = :v", id);
            exec("delete from Notification n where n.finalUser.id = :v", id);
            deletePictures("p.finalUser.id = :v", id, mediaUrls);

            entityManager.flush();
            entityManager.clear();

            FinalUser fresh = findUser(id);
            fresh.setName("Usuario eliminado");
            fresh.setEmail("deleted-u" + id + "@deleted.invalid");
            fresh.setPhoneNumber(null);
            fresh.setPassword(UUID.randomUUID().toString()); // no es un hash valido: nadie puede iniciar sesion
            fresh.setProfilePicture(null);
            fresh.setFcmToken(null);
            fresh.setEmailVerified(false);
            fresh.setPhoneVerified(false);
            fresh.setStatus(UserStatus.BLOCKED);
            fresh.setRoles(new HashSet<>(Set.of(roleService.getOrCreateRole(ROLE_DELETED))));
            finalUserRepository.save(fresh);
            entityManager.flush();
        } catch (DataIntegrityViolationException e) {
            log.error("No se pudo anonimizar el usuario {}: {}", id, e.getMostSpecificCause().getMessage());
            throw new ConflictException("No se pudo eliminar el usuario: quedaron datos relacionados que no se pudieron limpiar.");
        }
        deleteMedia(mediaUrls);
        log.info("Usuario {} ({}) eliminado: datos personales borrados, {} pago(s) conservado(s) por {} anios",
                id, email, paymentIds.size(), RETENTION_YEARS);
    }

    // ------------------------------------------------------------------
    // Vencimiento del plazo de conservacion
    // ------------------------------------------------------------------

    /**
     * Borra definitivamente los registros transaccionales de cuentas ya eliminadas cuya
     * fecha de pago supera RETENTION_YEARS anios, y luego las cuentas "tumba" que se
     * quedaron sin pagos. Lo invoca el barrido semanal. Devuelve cuantos pagos se borraron.
     */
    @Transactional
    public int purgeExpiredRetainedRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusYears(RETENTION_YEARS);
        List<String> mediaUrls = new ArrayList<>();

        // 1) Pagos de clientes eliminados: se borra el pago junto con su solicitud.
        int removed = purgeExpiredPayments(cutoff, true,
                "p.finalUser.id in (select u.id from FinalUser u join u.roles r where r.name = :role)", mediaUrls);
        // 2) Pagos de proveedores eliminados (cliente activo): se borra el pago y la oferta,
        //    la solicitud del cliente se conserva sin proveedor.
        removed += purgeExpiredPayments(cutoff, false,
                "p.serviceProvider.id in (select s.id from ServiceProvider s join s.roles r where r.name = :role)", mediaUrls);
        entityManager.flush();

        // 3) Cuentas tumba que ya no tienen ningun pago: se borran definitivamente.
        List<Long> userIds = ids("select u.id from FinalUser u join u.roles r where r.name = :v "
                + "and not exists (select p.id from Payment p where p.finalUser.id = u.id)", ROLE_DELETED);
        for (Long userId : userIds) {
            hardDeleteFinalUser(findUser(userId));
        }
        List<Long> providerIds = ids("select s.id from ServiceProvider s join s.roles r where r.name = :v "
                + "and not exists (select p.id from Payment p where p.serviceProvider.id = s.id)", ROLE_DELETED);
        for (Long providerId : providerIds) {
            hardDeleteProvider(findProvider(providerId));
        }

        deleteMedia(mediaUrls);
        log.info("Barrido de retencion: {} pago(s), {} cliente(s) y {} proveedor(es) eliminados definitivamente",
                removed, userIds.size(), providerIds.size());
        return removed;
    }

    private int purgeExpiredPayments(LocalDateTime cutoff, boolean clientIsGone, String partyCondition, List<String> mediaUrls) {
        List<Object[]> rows = entityManager.createQuery(
                        "select p.id, o.id, r.id from Payment p left join p.offer o left join o.serviceRequest r "
                                + "where p.paymentDate < :cutoff and " + partyCondition, Object[].class)
                .setParameter("cutoff", cutoff)
                .setParameter("role", ROLE_DELETED)
                .getResultList();
        for (Object[] row : rows) {
            Long paymentId = (Long) row[0];
            Long offerId = (Long) row[1];
            Long requestId = (Long) row[2];
            if (clientIsGone && requestId != null) {
                deleteRequests(new ArrayList<>(List.of(requestId)), mediaUrls);
            } else if (offerId != null) {
                if (requestId != null) {
                    exec("update ServiceRequest r set r.serviceProvider = null where r.id = :v", requestId);
                }
                deleteOffers(new ArrayList<>(List.of(offerId)), mediaUrls);
            } else {
                deletePayments(new ArrayList<>(List.of(paymentId)), mediaUrls);
            }
        }
        return rows.size();
    }

    // ------------------------------------------------------------------
    // Piezas de borrado en cadena (el orden importa por las claves foraneas)
    // ------------------------------------------------------------------

    /** Solicitudes completas: sus ofertas, pagos, notificaciones, comentario y fotos. */
    private void deleteRequests(List<Long> requestIds, List<String> mediaUrls) {
        if (requestIds.isEmpty()) {
            return;
        }
        deleteOffers(ids("select o.id from Offer o where o.serviceRequest.id in :v", requestIds), mediaUrls);
        exec("delete from Notification n where n.serviceRequest.id in :v", requestIds);
        exec("delete from Comment c where c.request.id in :v", requestIds);
        deletePictures("p.serviceRequest.id in :v", requestIds, mediaUrls);
        exec("delete from ServiceRequest r where r.id in :v", requestIds);
    }

    /** Ofertas: primero sus pagos, y se rompe la referencia circular solicitud -> oferta aceptada. */
    private void deleteOffers(List<Long> offerIds, List<String> mediaUrls) {
        if (offerIds.isEmpty()) {
            return;
        }
        deletePayments(ids("select p.id from Payment p where p.offer.id in :v", offerIds), mediaUrls);
        exec("update ServiceRequest r set r.acceptedOffer = null where r.acceptedOffer.id in :v", offerIds);
        exec("delete from Notification n where n.offer.id in :v", offerIds);
        exec("delete from Offer o where o.id in :v", offerIds);
    }

    /** Pagos: su comprobante (foto) y sus notificaciones. */
    private void deletePayments(List<Long> paymentIds, List<String> mediaUrls) {
        if (paymentIds.isEmpty()) {
            return;
        }
        deletePictures("p.payment.id in :v", paymentIds, mediaUrls);
        exec("delete from Notification n where n.payment.id in :v", paymentIds);
        exec("delete from Payment p where p.id in :v", paymentIds);
    }

    /** Borra las filas de Picture que cumplan la condicion y anota las URL para borrar el archivo despues. */
    private void deletePictures(String condition, Object value, List<String> mediaUrls) {
        List<Object[]> rows = entityManager
                .createQuery("select p.id, p.url from Picture p where " + condition, Object[].class)
                .setParameter("v", value)
                .getResultList();
        if (rows.isEmpty()) {
            return;
        }
        List<Long> pictureIds = new ArrayList<>();
        for (Object[] row : rows) {
            pictureIds.add((Long) row[0]);
            if (row[1] != null) {
                mediaUrls.add((String) row[1]);
            }
        }
        exec("delete from Picture p where p.id in :v", pictureIds);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private void deleteMedia(List<String> mediaUrls) {
        for (String url : mediaUrls) {
            try {
                mediaRepository.deleteImage(url);
            } catch (Exception e) {
                // No se cancela el borrado de la cuenta por un archivo que no se pudo borrar.
                log.warn("No se pudo borrar el archivo {}: {}", url, e.getMessage());
            }
        }
    }

    private ServiceProvider findProvider(Long id) {
        return serviceProviderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service provider with id " + id + " not found"));
    }

    private FinalUser findUser(Long id) {
        return finalUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User with id: " + id + " not found"));
    }

    private boolean hasRole(FinalUser user, String roleName) {
        return user.getRoles() != null && user.getRoles().stream().anyMatch(r -> roleName.equals(r.getName()));
    }

    private List<Long> ids(String jpql, Object value) {
        return entityManager.createQuery(jpql, Long.class).setParameter("v", value).getResultList();
    }

    private int exec(String jpql, Object value) {
        return entityManager.createQuery(jpql).setParameter("v", value).executeUpdate();
    }
}
