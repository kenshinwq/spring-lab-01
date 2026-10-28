package kz.iitu.springlab.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logActionRequiresNew(String action, String details) {
        return auditLogRepository.save(new AuditLog(action, details));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AuditLog logActionRequired(String action, String details) {
        return auditLogRepository.save(new AuditLog(action, details));
    }
}
