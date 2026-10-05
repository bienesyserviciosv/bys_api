package app.bys.bys_api.service;

import app.bys.bys_api.error.ConflictException;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.MediaRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
 * Decision de Lachy: al eliminar una cuenta se borra TODO lo relacionado
 * (solicitudes, ofertas, pagos, comentarios, notificaciones y fotos).
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

    private final EntityManager entityManager;
    private final FinalUserRepository finalUserRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final MediaRepository mediaRepository;

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

    /** Elimina un proveedor y todo lo relacionado con el. */
    @Transactional
    public void deleteProvider(Long id) {
        ServiceProvider provider = serviceProviderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service provider with id " + id + " not found"));
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
            ServiceProvider fresh = serviceProviderRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Service provider with id " + id + " not found"));
            serviceProviderRepository.delete(fresh);
            entityManager.flush();
        } catch (DataIntegrityViolationException e) {
            log.error("No se pudo eliminar el proveedor {}: {}", id, e.getMostSpecificCause().getMessage());
            throw new ConflictException("No se pudo eliminar el proveedor: quedaron datos relacionados que no se pudieron limpiar.");
        }
        deleteMedia(mediaUrls);
        log.info("Proveedor {} ({}) eliminado por el super-admin", id, provider.getEmail());
    }

    // ------------------------------------------------------------------
    // Borrado de un FinalUser (cliente o administrador)
    // ------------------------------------------------------------------

    private void purgeFinalUser(FinalUser user) {
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

            FinalUser fresh = findUser(id);
            finalUserRepository.delete(fresh);
            entityManager.flush();
        } catch (DataIntegrityViolationException e) {
            log.error("No se pudo eliminar el usuario {}: {}", id, e.getMostSpecificCause().getMessage());
            throw new ConflictException("No se pudo eliminar el usuario: quedaron datos relacionados que no se pudieron limpiar.");
        }
        deleteMedia(mediaUrls);
        log.info("Usuario {} ({}) eliminado por el super-admin", id, email);
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
