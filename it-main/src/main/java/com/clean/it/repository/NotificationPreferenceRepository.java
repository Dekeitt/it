package com.clean.it.repository;

import com.clean.it.domain.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference,Long> {
}
