package app.bys.bys_api.service;

import app.bys.bys_api.mapper.OfferMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.OfferDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.repository.OfferRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepo;
    private final OfferMapper offerMapper;

    public OfferDto get(Long id) {
        return offerMapper.entityToDto(offerRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offer with id: " + id + " not found")));
    }

    public PageDto<OfferDto> getAll(Pageable pageable, String search) {
        return PageMapper.pageToDto(offerRepo.findAll(pageable).map(offerMapper::entityToDto));
    }



}
