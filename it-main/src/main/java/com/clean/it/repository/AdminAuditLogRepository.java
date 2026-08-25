package com.clean.it.repository;

import com.clean.it.domain.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog,Long> {
 Optional<AdminAuditLog> findByIdempotencyKey(String idempotencyKey);
 List<AdminAuditLog> findTop100ByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType,String targetId);
}
