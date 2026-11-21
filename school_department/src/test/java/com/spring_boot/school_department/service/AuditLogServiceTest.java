package com.spring_boot.school_department.service;

import com.spring_boot.school_department.entity.AuditLog;
import com.spring_boot.school_department.enums.AuditStatus;
import com.spring_boot.school_department.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void log_validData_savesAuditLog() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        auditLogService.log("CREATE", "Student", 5L, AuditStatus.SUCCESS, "student yaradildi");

        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getEntityName()).isEqualTo("Student");
        assertThat(saved.getEntityId()).isEqualTo(5L);
        assertThat(saved.getStatus()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(saved.getMessage()).isEqualTo("student yaradildi");
    }
}
