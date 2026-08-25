package com.clean.it.service;

import com.clean.it.dto.AppDtos.CleanerDto;
import java.util.List;

public interface CleanerService {
    List<CleanerDto> listCleaners();
    CleanerDto createCleaner(Long userId, String email, CleanerDto dto);
}
