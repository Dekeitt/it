package com.clean.it.service.impl;

import com.clean.it.domain.Cleaner;
import com.clean.it.dto.AppDtos.CleanerDto;
import com.clean.it.repository.CleanerRepository;
import com.clean.it.service.CleanerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CleanerServiceImpl implements CleanerService {

    private final CleanerRepository cleanerRepository;

    public CleanerServiceImpl(CleanerRepository cleanerRepository) {
        this.cleanerRepository = cleanerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CleanerDto> listCleaners() {
        return cleanerRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public CleanerDto createCleaner(CleanerDto dto) {
        Cleaner cleaner = cleanerRepository.findByEmailIgnoreCase(dto.getEmail()).orElseGet(Cleaner::new);
        cleaner.setEmail(dto.getEmail());
        cleaner.setName(dto.getName());
        if (cleaner.getRating() == null) {
            cleaner.setRating(0.0);
        }
        return toDto(cleanerRepository.save(cleaner));
    }

    private CleanerDto toDto(Cleaner cleaner) {
        CleanerDto dto = new CleanerDto();
        dto.setId(cleaner.getId());
        dto.setEmail(cleaner.getEmail());
        dto.setName(cleaner.getName());
        dto.setRating(cleaner.getRating());
        return dto;
    }
}
