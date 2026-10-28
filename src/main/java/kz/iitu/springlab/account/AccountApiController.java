package kz.iitu.springlab.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.flywaydb.core.Flyway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts & Transactions (Lab 9)", description = "Declarative transactions, propagation, optimistic locking, and Flyway migrations")
public class AccountApiController {

    private final AccountService accountService;
    private final AuditLogRepository auditLogRepository;
    private final AccountRepository accountRepository;
    private final Optional<Flyway> flyway;

    public AccountApiController(AccountService accountService,
                                AuditLogRepository auditLogRepository,
                                AccountRepository accountRepository,
                                Optional<Flyway> flyway) {
        this.accountService = accountService;
        this.auditLogRepository = auditLogRepository;
        this.accountRepository = accountRepository;
        this.flyway = flyway;
    }

    @Operation(summary = "Get all accounts with balance, status, and version")
    @GetMapping
    public List<Account> getAllAccounts() {
        return accountService.findAll();
    }

    @Operation(summary = "Task A: Transfer money atomically between accounts")
    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transfer(
            @RequestParam(required = false) Long fromId,
            @RequestParam(required = false) Long toId,
            @RequestParam(defaultValue = "100.00") BigDecimal amount) {

        if (fromId == null || toId == null) {
            Account alice = accountRepository.findByOwnerName("Alice").orElseThrow();
            Account bob = accountRepository.findByOwnerName("Bob").orElseThrow();
            fromId = alice.getId();
            toId = bob.getId();
        }

        accountService.transfer(fromId, toId, amount);
        Account from = accountService.findById(fromId);
        Account to = accountService.findById(toId);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Transferred " + amount + " from " + from.getOwnerName() + " to " + to.getOwnerName(),
                "fromAccount", from,
                "toAccount", to
        ));
    }

    @Operation(summary = "Task A: Transfer with simulated failure to demonstrate rollback")
    @PostMapping("/transfer-fail")
    public ResponseEntity<Map<String, Object>> transferFail(
            @RequestParam(required = false) Long fromId,
            @RequestParam(required = false) Long toId,
            @RequestParam(defaultValue = "100.00") BigDecimal amount) {

        if (fromId == null || toId == null) {
            Account alice = accountRepository.findByOwnerName("Alice").orElseThrow();
            Account bob = accountRepository.findByOwnerName("Bob").orElseThrow();
            fromId = alice.getId();
            toId = bob.getId();
        }

        try {
            accountService.transferWithUncheckedException(fromId, toId, amount);
            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (Exception e) {
            Account from = accountService.findById(fromId);
            Account to = accountService.findById(toId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "ROLLED_BACK",
                    "error", e.getMessage(),
                    "fromAccountBalance", from.getBalance(),
                    "toAccountBalance", to.getBalance(),
                    "explanation", "Transaction was automatically rolled back due to unhandled RuntimeException!"
            ));
        }
    }

    @Operation(summary = "Task B: Propagation demo comparing REQUIRED vs REQUIRES_NEW")
    @PostMapping("/propagation-demo")
    public ResponseEntity<Map<String, Object>> propagationDemo(
            @RequestParam(defaultValue = "true") boolean requiresNew,
            @RequestParam(defaultValue = "true") boolean failOuter) {

        Account alice = accountRepository.findByOwnerName("Alice").orElseThrow();
        Account bob = accountRepository.findByOwnerName("Bob").orElseThrow();
        Long fromId = alice.getId();
        Long toId = bob.getId();

        BigDecimal amount = new BigDecimal("50.00");
        long auditCountBefore = auditLogRepository.count();

        String resultStatus;
        String message;

        try {
            if (requiresNew) {
                accountService.transferWithAuditRequiresNew(fromId, toId, amount, failOuter);
            } else {
                accountService.transferWithAuditRequired(fromId, toId, amount, failOuter);
            }
            resultStatus = "SUCCESS";
            message = "Transaction committed normally";
        } catch (Exception ex) {
            resultStatus = "OUTER_TRANSACTION_FAILED";
            message = ex.getMessage();
        }

        long auditCountAfter = auditLogRepository.count();
        boolean auditLogPreserved = auditCountAfter > auditCountBefore;

        return ResponseEntity.ok(Map.of(
                "propagationMode", requiresNew ? "REQUIRES_NEW" : "REQUIRED",
                "failOuterTriggered", failOuter,
                "transactionResult", resultStatus,
                "auditLogsCountBefore", auditCountBefore,
                "auditLogsCountAfter", auditCountAfter,
                "auditLogPreserved", auditLogPreserved,
                "explanation", requiresNew
                        ? "REQUIRES_NEW committed audit in independent transaction; it survived outer rollback!"
                        : "REQUIRED participated in outer transaction; it was rolled back together!"
        ));
    }

    @Operation(summary = "Task B: Get all audit log entries")
    @GetMapping("/audit-logs")
    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAll();
    }

    @Operation(summary = "Task D: Optimistic Locking conflict simulation with @Version")
    @PostMapping("/optimistic-lock-demo")
    public ResponseEntity<Map<String, Object>> optimisticLockDemo() {
        Account alice = accountRepository.findByOwnerName("Alice").orElseThrow();
        Long aliceId = alice.getId();

        Account copy1 = accountRepository.findById(aliceId).orElseThrow();
        Account copy2 = accountRepository.findById(aliceId).orElseThrow();

        // 1. Commit update with copy1
        copy1.credit(new BigDecimal("10.00"));
        accountRepository.saveAndFlush(copy1);

        // 2. Attempt to commit stale copy2 (it has older version)
        try {
            copy2.debit(new BigDecimal("20.00"));
            accountRepository.saveAndFlush(copy2);
            return ResponseEntity.ok(Map.of("status", "UNEXPECTED_SUCCESS"));
        } catch (Exception ex) {
            Account currentInDb = accountRepository.findById(aliceId).orElseThrow();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "OPTIMISTIC_LOCK_CONFLICT_DETECTED",
                    "exceptionType", ex.getClass().getSimpleName(),
                    "message", "Prevented lost update! Stale version was rejected by @Version check.",
                    "currentVersionInDb", currentInDb.getVersion(),
                    "currentBalanceInDb", currentInDb.getBalance()
            ));
        }
    }

    @Operation(summary = "Task E: View applied Flyway schema migrations")
    @GetMapping("/migrations")
    public List<Map<String, Object>> getFlywayMigrations() {
        if (flyway.isEmpty()) {
            return List.of(Map.of("status", "Flyway not configured"));
        }
        return Arrays.stream(flyway.get().info().all())
                .map(m -> Map.<String, Object>of(
                        "version", m.getVersion() != null ? m.getVersion().toString() : "N/A",
                        "description", m.getDescription(),
                        "type", m.getType().name(),
                        "state", m.getState().name(),
                        "installedOn", m.getInstalledOn() != null ? m.getInstalledOn().toString() : "N/A"
                ))
                .toList();
    }
}
