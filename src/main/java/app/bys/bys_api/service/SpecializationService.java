package app.bys.bys_api.service;

import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.mapper.SpecializationMapper;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.SpecializationDto;
import app.bys.bys_api.model.entity.Specialization;
import app.bys.bys_api.repository.SpecializationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpecializationService {

    private final SpecializationRepository specializationRepository;
    private final SpecializationMapper mapper;

    public SpecializationDto get(Long id) {
        return mapper.entityToDto(specializationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Specialization with id " + id + " not found")));
    }

    public PageDto<SpecializationDto> getAll(Pageable pageable) {
        return PageMapper.pageToDto(specializationRepository.findAll(pageable).map(mapper::entityToDto));
    }

    public SpecializationDto create(SpecializationDto specializationDto) {
        return mapper.entityToDto(specializationRepository.save(mapper.dtoToEntity(specializationDto)));
    }

    public SpecializationDto update(Long id, SpecializationDto specializationDto) {
        Specialization specializationFound = specializationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Specialization with id " + id + " not found"));

        mapper.updateSpecializationFromDto(specializationDto, specializationFound);
        return mapper.entityToDto(specializationRepository.save(specializationFound));
    }

    public void delete(Long id) {
        if (!specializationRepository.existsById(id)) {
            throw new EntityNotFoundException("Specialization with id " + id + " not found");
        }
        specializationRepository.deleteById(id);
    }

}
