package com.spring_boot.school_department.service;

import com.spring_boot.school_department.entity.AuditLog;
import com.spring_boot.school_department.enums.AuditStatus;
import com.spring_boot.school_department.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // REQUIRES_NEW - basqa transaction icinde olsada, oz transaction yaratmasi ucun,
    // eger basqa transaction icinde rollback olsa yenede audit yazilmalidi cunki.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityName, Long entityId, AuditStatus status, String message) {

        AuditLog auditLog = new AuditLog();

        auditLog.setAction(action);
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId);
        auditLog.setStatus(status);
        auditLog.setMessage(message);

        auditLogRepository.save(auditLog);
    }
}
