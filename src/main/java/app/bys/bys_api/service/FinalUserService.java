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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FinalUserService {

    private final FinalUserRepository finalUserRepository;
    private final FinalUserMapper mapper;

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
                orElseThrow(()-> new EntityNotFoundException("Final user with id: " + id + " not found"));
        mapper.updateFinalUserFromDto(finalUserDto, userFound);
        return mapper.entityToDto(finalUserRepository.save(userFound));
    }

    public void delete(Long id) {
        if (!finalUserRepository.existsById(id)) {
            throw new EntityNotFoundException("Final user with id: " + id + " not found");
        }
        finalUserRepository.deleteById(id);
    }
}
