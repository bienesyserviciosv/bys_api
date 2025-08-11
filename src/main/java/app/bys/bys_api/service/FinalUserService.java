package app.bys.bys_api.service;

import app.bys.bys_api.error.DuplicateEmailException;
import app.bys.bys_api.mapper.FinalUserMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.FinalUserDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.repository.FinalUserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinalUserService {

    private final FinalUserRepository finalUserRepository;
    private final FinalUserMapper mapper;
    private final RoleService roleService;

    public FinalUserDto get(Long id) {
        return mapper.entityToDto(finalUserRepository.findById(id).
                orElseThrow(() -> new EntityNotFoundException("Final user with id: " + id + " not found")));
    }

    public PageDto<FinalUserDto> getAll(Pageable pageable) {
        return PageMapper.pageToDto(finalUserRepository.findAll(pageable).map(mapper::entityToDto));
    }

    public FinalUserDto create(FinalUserDto finalUserDto) {
        if (finalUserRepository.existsByEmail(finalUserDto.getEmail())) {
            throw new DuplicateEmailException("The email is already registered");
        }
        return mapper.entityToDto(finalUserRepository.save(mapper.dtoToEntity(finalUserDto)));
    }

    public FinalUserDto update(Long id, FinalUserDto finalUserDto) {
        FinalUser userFound = finalUserRepository.findById(id).
                orElseThrow(() -> new EntityNotFoundException("Final user with id: " + id + " not found"));
        mapper.updateFinalUserFromDto(finalUserDto, userFound);
        return mapper.entityToDto(finalUserRepository.save(userFound));
    }

    public void delete(Long id) {
        if (!finalUserRepository.existsById(id)) {
            throw new EntityNotFoundException("Final user with id: " + id + " not found");
        }
        finalUserRepository.deleteById(id);
    }

    public FinalUser findOrCreateUser(String email, String name) {
        log.info("Recibido email: " + email + ", nombre: " + name);
        return finalUserRepository.findByEmail(email)
                .orElseGet(() -> {
                    FinalUser newUser = new FinalUser();
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setRoles(Set.of(roleService.getRoleOrThrow("ROLE_USER")));
                    return finalUserRepository.save(newUser);

        /*if (existingUser != null) {
            return existingUser;
        }
        FinalUser newUser = new FinalUser();
        newUser.setEmail(email);
        newUser.setName(name);
        newUser.setRoles(Set.of(roleService.getRoleOrThrow("ROLE_USER")));

        return finalUserRepository.save(newUser);
    */
                });
    }
}
