package com.clean.it.service;

import com.clean.it.domain.NotificationPreference;
import com.clean.it.dto.NotificationDtos.PreferenceRequest;
import com.clean.it.dto.NotificationDtos.PreferenceResponse;
import com.clean.it.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferenceService {
    private final NotificationPreferenceRepository repository;
    public NotificationPreferenceService(NotificationPreferenceRepository repository){this.repository=repository;}

    @Transactional(readOnly=true)
    public PreferenceResponse get(Long userId){return toDto(repository.findById(userId).orElseGet(()->defaults(userId)));}

    @Transactional
    public PreferenceResponse update(Long userId,PreferenceRequest request){
        NotificationPreference preference=repository.findById(userId).orElseGet(()->defaults(userId));
        preference.setEmailEnabled(request.isEmailEnabled());preference.setPushEnabled(request.isPushEnabled());
        return toDto(repository.save(preference));
    }

    @Transactional(readOnly=true)
    public boolean emailEnabled(Long userId){return repository.findById(userId).map(NotificationPreference::isEmailEnabled).orElse(true);}
    @Transactional(readOnly=true)
    public boolean pushEnabled(Long userId){return repository.findById(userId).map(NotificationPreference::isPushEnabled).orElse(true);}

    private NotificationPreference defaults(Long userId){NotificationPreference p=new NotificationPreference();p.setUserId(userId);return p;}
    private PreferenceResponse toDto(NotificationPreference p){PreferenceResponse r=new PreferenceResponse();r.setEmailEnabled(p.isEmailEnabled());r.setPushEnabled(p.isPushEnabled());return r;}
}
