package kz.iitu.springlab.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AuditService auditService;

    public AccountService(AccountRepository accountRepository, AuditService auditService) {
        this.accountRepository = accountRepository;
        this.auditService = auditService;
    }

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
    }

    public Account findByOwnerName(String ownerName) {
        return accountRepository.findByOwnerName(ownerName)
                .orElseThrow(() -> new IllegalArgumentException("Account not found for owner: " + ownerName));
    }

    // Task A: Standard declarative transfer with @Transactional
    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        Account from = findById(fromId);
        Account to = findById(toId);

        from.debit(amount);
        to.credit(amount);

        accountRepository.save(from);
        accountRepository.save(to);
    }

    // Task A: Simulated failure throwing RuntimeException (unchecked) after debit
    @Transactional
    public void transferWithUncheckedException(Long fromId, Long toId, BigDecimal amount) {
        Account from = findById(fromId);
        Account to = findById(toId);

        from.debit(amount);
        accountRepository.save(from);

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Simulated unexpected failure after debit - should trigger rollback");
        }

        to.credit(amount);
        accountRepository.save(to);
    }

    // Task A: Checked exception without rollbackFor (by default Spring does NOT rollback checked exceptions!)
    @Transactional
    public void transferWithCheckedExceptionNoRollback(Long fromId, Long toId, BigDecimal amount) throws Exception {
        Account from = findById(fromId);
        from.debit(amount);
        accountRepository.save(from);

        throw new Exception("Checked exception: default Spring behavior commits without rollback!");
    }

    // Task A: Checked exception with rollbackFor = Exception.class
    @Transactional(rollbackFor = Exception.class)
    public void transferWithCheckedExceptionWithRollback(Long fromId, Long toId, BigDecimal amount) throws Exception {
        Account from = findById(fromId);
        from.debit(amount);
        accountRepository.save(from);

        throw new Exception("Checked exception with rollbackFor: successfully triggers rollback!");
    }

    // Task B: Propagation REQUIRES_NEW for audit log
    @Transactional
    public void transferWithAuditRequiresNew(Long fromId, Long toId, BigDecimal amount, boolean failOuter) {
        auditService.logActionRequiresNew("TRANSFER_ATTEMPT",
                "From ID " + fromId + " to ID " + toId + ", Amount: " + amount);

        Account from = findById(fromId);
        Account to = findById(toId);

        from.debit(amount);
        to.credit(amount);
        accountRepository.save(from);
        accountRepository.save(to);

        if (failOuter) {
            throw new RuntimeException("Forced failure in outer transaction after REQUIRES_NEW audit log was committed");
        }
    }

    // Task B: Propagation REQUIRED for audit log
    @Transactional
    public void transferWithAuditRequired(Long fromId, Long toId, BigDecimal amount, boolean failOuter) {
        auditService.logActionRequired("TRANSFER_ATTEMPT",
                "From ID " + fromId + " to ID " + toId + ", Amount: " + amount);

        Account from = findById(fromId);
        Account to = findById(toId);

        from.debit(amount);
        to.credit(amount);
        accountRepository.save(from);
        accountRepository.save(to);

        if (failOuter) {
            throw new RuntimeException("Forced failure in outer transaction - REQUIRED audit log rolls back together");
        }
    }

    // Task C: Isolation level READ_COMMITTED
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BigDecimal getBalanceReadCommitted(Long accountId) {
        return findById(accountId).getBalance();
    }

    // Task C: Isolation level REPEATABLE_READ
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BigDecimal getBalanceRepeatableRead(Long accountId) {
        return findById(accountId).getBalance();
    }

    // Task D: Update account to demonstrate optimistic locking
    @Transactional
    public Account updateAccount(Account account) {
        return accountRepository.save(account);
    }
}
