package com.clean.it.service.impl;

import com.clean.it.dto.AppDtos.CleanerDto;
import com.clean.it.domain.Cleaner;
import com.clean.it.repository.CleanerRepository;
import com.clean.it.service.CleanerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CleanerServiceImpl implements CleanerService {

    private final CleanerRepository cleanerRepository;

    public CleanerServiceImpl(CleanerRepository cleanerRepository) {
        this.cleanerRepository = cleanerRepository;
    }

    @Override
    public List<CleanerDto> listCleaners() {
        return cleanerRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public CleanerDto createCleaner(CleanerDto dto) {
        Cleaner c = new Cleaner();
        c.setEmail(dto.getEmail());
        c.setName(dto.getName());
        c.setRating(dto.getRating());
        Cleaner saved = cleanerRepository.save(c);
        return toDto(saved);
    }

    private CleanerDto toDto(Cleaner c) {
        CleanerDto d = new CleanerDto();
        d.setId(c.getId());
        d.setEmail(c.getEmail());
        d.setName(c.getName());
        d.setRating(c.getRating());
        return d;
    }
}

