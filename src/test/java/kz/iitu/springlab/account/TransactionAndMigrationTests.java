package kz.iitu.springlab.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TransactionAndMigrationTests {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private org.flywaydb.core.Flyway flyway;

    private Long aliceId;
    private Long bobId;

    @BeforeEach
    void setUp() {
        flyway.migrate();
        auditLogRepository.deleteAll();
        accountRepository.deleteAll();

        Account alice = accountRepository.save(new Account("Alice", new BigDecimal("1000.00")));
        Account bob = accountRepository.save(new Account("Bob", new BigDecimal("500.00")));

        aliceId = alice.getId();
        bobId = bob.getId();
    }

    @Test
    @DisplayName("Task A: Successful transfer moves money atomically between accounts")
    void testSuccessfulTransfer() {
        accountService.transfer(aliceId, bobId, new BigDecimal("100.00"));

        Account alice = accountRepository.findById(aliceId).orElseThrow();
        Account bob = accountRepository.findById(bobId).orElseThrow();

        assertThat(alice.getBalance()).isEqualByComparingTo("900.00");
        assertThat(bob.getBalance()).isEqualByComparingTo("600.00");
    }

    @Test
    @DisplayName("Task A: Insufficient funds throws exception and rolls back both accounts")
    void testInsufficientFundsRollback() {
        assertThatThrownBy(() ->
                accountService.transfer(aliceId, bobId, new BigDecimal("2500.00"))
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("Insufficient funds");

        Account alice = accountRepository.findById(aliceId).orElseThrow();
        Account bob = accountRepository.findById(bobId).orElseThrow();

        assertThat(alice.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(bob.getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Task A: Unchecked exception after debit triggers automatic rollback")
    void testUncheckedExceptionTriggersRollback() {
        assertThatThrownBy(() ->
                accountService.transferWithUncheckedException(aliceId, bobId, new BigDecimal("100.00"))
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Simulated unexpected failure");

        Account alice = accountRepository.findById(aliceId).orElseThrow();
        Account bob = accountRepository.findById(bobId).orElseThrow();

        // Alice must NOT have lost 100.00 because transaction rolled back
        assertThat(alice.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(bob.getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Task B: Propagation REQUIRES_NEW commits audit log even when outer transaction fails")
    void testPropagationRequiresNewRetainsAuditLogOnOuterRollback() {
        long auditCountBefore = auditLogRepository.count();

        assertThatThrownBy(() ->
                accountService.transferWithAuditRequiresNew(aliceId, bobId, new BigDecimal("100.00"), true)
        ).isInstanceOf(RuntimeException.class);

        // Account balances rolled back
        Account alice = accountRepository.findById(aliceId).orElseThrow();
        assertThat(alice.getBalance()).isEqualByComparingTo("1000.00");

        // But audit log in separate transaction was committed!
        long auditCountAfter = auditLogRepository.count();
        assertThat(auditCountAfter).isEqualTo(auditCountBefore + 1);
    }

    @Test
    @DisplayName("Task B: Propagation REQUIRED rolls back audit log together with outer transaction")
    void testPropagationRequiredRollsBackAuditLogOnOuterRollback() {
        long auditCountBefore = auditLogRepository.count();

        assertThatThrownBy(() ->
                accountService.transferWithAuditRequired(aliceId, bobId, new BigDecimal("100.00"), true)
        ).isInstanceOf(RuntimeException.class);

        // Both accounts and audit log rolled back
        Account alice = accountRepository.findById(aliceId).orElseThrow();
        assertThat(alice.getBalance()).isEqualByComparingTo("1000.00");

        long auditCountAfter = auditLogRepository.count();
        assertThat(auditCountAfter).isEqualTo(auditCountBefore);
    }

    @Test
    @DisplayName("Task D: Optimistic locking with @Version detects concurrent update conflict")
    void testOptimisticLockingConflict() {
        Account aliceCopy1 = accountRepository.findById(aliceId).orElseThrow();
        Account aliceCopy2 = accountRepository.findById(aliceId).orElseThrow();

        // Transaction 1 updates and commits first
        aliceCopy1.credit(new BigDecimal("50.00"));
        accountRepository.saveAndFlush(aliceCopy1);

        // Transaction 2 tries to commit stale copy (version mismatch)
        aliceCopy2.debit(new BigDecimal("30.00"));
        assertThatThrownBy(() -> accountRepository.saveAndFlush(aliceCopy2))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Task E: Flyway schema migrations V1, V2, and V3 applied and recorded")
    void testFlywayMigrationsApplied() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT version, description, type, script, success FROM flyway_schema_history ORDER BY installed_rank"
        );

        assertThat(rows).isNotEmpty();
        assertThat(rows.stream().anyMatch(r -> "1".equals(r.get("version")))).isTrue();
        assertThat(rows.stream().anyMatch(r -> "2".equals(r.get("version")))).isTrue();
        assertThat(rows.stream().anyMatch(r -> "3".equals(r.get("version")))).isTrue();

        // Check status column from V2 exists
        Account alice = accountRepository.findById(aliceId).orElseThrow();
        assertThat(alice.getStatus()).isEqualTo("ACTIVE");
    }
}
