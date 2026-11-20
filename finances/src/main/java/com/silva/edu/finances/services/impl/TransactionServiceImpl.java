package com.silva.edu.finances.services.impl;

import com.silva.edu.finances.domain.dtos.request.AddTransactionRequestDTO;
import com.silva.edu.finances.domain.dtos.request.UpdateTransactionRequestDTO;
import com.silva.edu.finances.domain.dtos.response.TransactionResponseDTO;
import com.silva.edu.finances.domain.entities.Category;
import com.silva.edu.finances.domain.entities.CreditCard;
import com.silva.edu.finances.domain.entities.Recurrence;
import com.silva.edu.finances.domain.entities.Transaction;
import com.silva.edu.finances.domain.enums.RecurrenceStatus;
import com.silva.edu.finances.domain.enums.RecurrenceType;
import com.silva.edu.finances.domain.enums.TransactionEntryType;
import com.silva.edu.finances.domain.enums.TransactionStatus;
import com.silva.edu.finances.infra.exceptions.CustomExceptions;
import com.silva.edu.finances.repositories.CreditCardRepository;
import com.silva.edu.finances.repositories.RecurrenceRepository;
import com.silva.edu.finances.repositories.TransactionRepository;
import com.silva.edu.finances.services.CategoryService;
import com.silva.edu.finances.services.TransactionService;
import com.silva.edu.finances.services.FinanceAccountService;
import lombok.NonNull;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;
    private final CategoryService categoryService;
    private final RecurrenceRepository recurrenceRepository;
    private final FinanceAccountService accountService;
    private final CreditCardRepository creditCardRepository;
    private final CreditCardInvoiceService invoices;

    public TransactionServiceImpl(TransactionRepository repository, CategoryService categoryService,
                                  RecurrenceRepository recurrenceRepository, FinanceAccountService accountService,
                                  CreditCardRepository creditCardRepository, CreditCardInvoiceService invoices) {
        this.repository = repository;
        this.categoryService = categoryService;
        this.recurrenceRepository = recurrenceRepository;
        this.accountService = accountService;
        this.creditCardRepository = creditCardRepository;
        this.invoices = invoices;
    }

    @Override
    public TransactionResponseDTO save(AddTransactionRequestDTO request) {
        Transaction transaction = new Transaction();
        BeanUtils.copyProperties(request, transaction);
        transaction.setStatus(request.status() == null ? TransactionStatus.OK : request.status());
        if (transaction.getStatus() == TransactionStatus.DELETED)
            throw new IllegalArgumentException("Transactions cannot be created as DELETED");
        String accountId = request.accountId() == null || request.accountId().isBlank()
                ? accountService.getOrCreatePersonalDefault() : request.accountId().trim();
        accountService.requireAccess(accountId);
        transaction.setAccountId(accountId);
        UUID dataOwner = accountService.ownerOf(accountId);
        transaction.setEntryType(TransactionEntryType.NORMAL);

        if (request.categoryID() != null) {
            Category category = findCategory(request.categoryID());
            requireCategoryDirection(category, request.income());
            transaction.setCategory(category);
        }

        if (request.recurrencyID() != null) {
            Recurrence rec = recurrenceRepository.findById(request.recurrencyID())
                    .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Recurrence.class.getSimpleName(), request.recurrencyID()));
            if (!accountId.equals(rec.getAccountId()))
                throw new IllegalArgumentException("Recurrence does not belong to the informed account");
            transaction.setRecurrence(rec);

        }
        if (request.creditCardId() != null) {
            // Cartão é forma de pagamento: a compra continua na conta do cartão (v1 é só despesa).
            if (request.income())
                throw new IllegalArgumentException("Card purchases must be expenses");
            CreditCard card = requireCardOfAccount(request.creditCardId(), accountId);
            transaction.setCreditCard(card);
            transaction.setEntryType(TransactionEntryType.CARD);
            transaction.setIncome(false);
        }
        transaction.setUserId(dataOwner);

        repository.save(transaction);
        return new TransactionResponseDTO(transaction);
    }

    @Override
    public void delete(UUID id) {
        Transaction transaction = findEntity(id);
        requireEditableByCrud(transaction);
        lockPaidCycle(transaction, BigDecimal.ZERO);
        repository.delete(transaction);
    }

    @Override
    public Page<@NonNull TransactionResponseDTO> findAll(int page, int size, String accountId) {
        if (accountId != null && !accountId.isBlank()) accountService.requireAccess(accountId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("executionDate")));
        var result = accountId == null || accountId.isBlank()
                ? repository.findAllByAccountIdIn(accountService.accessibleAccountIds(), pageable)
                : repository.findAllByAccountId(accountId, pageable);
        Map<UUID, String> counterparts = counterparts(result.getContent());
        return result.map(transaction -> response(transaction, counterparts));
    }

    @Override
    public TransactionResponseDTO findById(UUID id) {
        return response(findEntity(id), null);
    }

    @Override
    public TransactionResponseDTO update(UUID id, UpdateTransactionRequestDTO request) {
        Transaction transaction = findEntity(id);
        requireEditableByCrud(transaction);
        boolean confirming = TransactionStatus.OK.equals(request.status())
                && transaction.getStatus() == TransactionStatus.PENDING;
        // Trocar o cartão ou voltar a NORMAL sai do ciclo, então o teto pode voltar a zero.
        BigDecimal newValue = request.value();
        lockPaidCycle(transaction, confirming ? BigDecimal.ZERO : newValue);
        transaction.setValue(request.value());
        transaction.setTitle(request.title());
        if (request.executionDate() != null) {
            transaction.setExecutionDate(request.executionDate());
        }
        if (request.creditCardId() != null) {
            CreditCard card = requireCardOfAccount(request.creditCardId(),
                    transaction.getAccountId());
            transaction.setCreditCard(card);
            transaction.setEntryType(TransactionEntryType.CARD);
            transaction.setIncome(false);
        } else if (Boolean.TRUE.equals(request.clearCreditCard()) && transaction.getCreditCard() != null) {
            transaction.setCreditCard(null);
            transaction.setEntryType(TransactionEntryType.NORMAL);
        }
        if (request.categoryID() != null) {
            Category category = findCategory(request.categoryID());
            requireCategoryDirection(category, request.income() == null
                    ? isCardPurchase(transaction) || category.isIncome() : request.income());
            transaction.setCategory(category);
            transaction.setIncome(isCardPurchase(transaction) ? false : category.isIncome());
            syncRecurrenceCategory(transaction, category);
        } else if (request.income() != null && transaction.getCategory() == null) {
            transaction.setIncome(isCardPurchase(transaction) ? false : request.income());
        }
        if (request.status() != null) {
            if (request.status() == TransactionStatus.DELETED)
                throw new IllegalArgumentException("Use the delete endpoint to remove a transaction");
            transaction.setStatus(request.status());
        }
        return new TransactionResponseDTO(repository.save(transaction));
    }

    @Override
    public TransactionResponseDTO confirm(UUID id, java.math.BigDecimal value) {
        Transaction transaction = findEntity(id);
        if (transaction.getStatus() != TransactionStatus.PENDING)
            throw new IllegalArgumentException("Only pending transactions can be confirmed");
        if (value != null) {
            if (value.signum() <= 0)
                throw new IllegalArgumentException("Transaction value must be greater than zero");
            transaction.setValue(value);
        }
        transaction.setStatus(TransactionStatus.OK);
        Recurrence recurrence = transaction.getRecurrence();
        if (recurrence != null && recurrence.getType() == RecurrenceType.INSTALLMENT) {
            int payed = (recurrence.getPayed() == null ? 0 : recurrence.getPayed()) + 1;
            recurrence.setPayed(payed);
            if (recurrence.getInstallments() != null && payed >= recurrence.getInstallments()) {
                recurrence.setStatus(RecurrenceStatus.PAYED);
            }
            recurrenceRepository.save(recurrence);
        }
        return new TransactionResponseDTO(repository.save(transaction));
    }

    private Transaction findEntity(UUID id) {
        Transaction transaction = repository.findById(id)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(Transaction.class.getSimpleName(), id));
        if (transaction.getAccountId() == null) {
            if (!accountService.visibleUserIds().contains(transaction.getUserId()))
                throw new CustomExceptions.EntityNotFoundException(Transaction.class.getSimpleName(), id);
        } else {
            accountService.requireAccess(transaction.getAccountId());
        }
        return transaction;
    }

    /** Transferência e pagamento de fatura têm endpoints próprios (reabriria o par / a fatura). */
    private void requireEditableByCrud(Transaction transaction) {
        if (transaction.getEntryType() == TransactionEntryType.TRANSFER)
            throw new IllegalArgumentException("Transfers must be changed through the transfers endpoint");
        if (transaction.getEntryType() == TransactionEntryType.INVOICE_PAYMENT)
            throw new IllegalArgumentException("Invoice payments must be changed through the card endpoints");
    }

    /**
     * Compra CARD de ciclo com pagamento ativo fica travada enquanto a fatura fechada segue paga;
     * em ciclo parcialmente pago, o novo valor não pode deixar o total abaixo do já pago.
     * {@code newValue} é o valor que a compra terá depois da operação (zero em exclusão).
     */
    private void lockPaidCycle(Transaction transaction, BigDecimal newValue) {
        if (!isCardPurchase(transaction)) return;
        CreditCard card = transaction.getCreditCard();
        UUID owner = accountService.ownerOf(card.getAccountId());
        YearMonth month = CreditCardInvoiceCalculator.cycleOf(
                CreditCardInvoiceCalculator.toLocalDate(transaction.getExecutionDate()),
                card.getClosingDay(), card.getDueDay()).invoiceMonth();
        CreditCardInvoiceService.Invoice invoice = invoices.invoice(card,
                invoices.ledger(card, owner, LocalDate.now(ZoneOffset.UTC)), month);
        BigDecimal paid = invoice.paid();
        if (paid.signum() <= 0) return;
        boolean cyclePaid = invoice.total().subtract(paid).signum() <= 0;
        if (cyclePaid)
            throw new IllegalArgumentException("Card purchases of a paid invoice cannot be changed while the payment exists");
        BigDecimal total = invoice.total().subtract(transaction.getValue() == null ? BigDecimal.ZERO : transaction.getValue());
        BigDecimal newTotal = total.add(newValue == null ? BigDecimal.ZERO : newValue);
        if (newTotal.compareTo(paid) < 0)
            throw new IllegalArgumentException("Cycle total cannot be lower than the amount already paid");
    }

    private CreditCard requireCardOfAccount(UUID creditCardId, String accountId) {
        CreditCard card = creditCardRepository.findById(creditCardId)
                .orElseThrow(() -> new CustomExceptions.EntityNotFoundException(CreditCard.class.getSimpleName(), creditCardId));
        if (accountId == null || !accountId.equals(card.getAccountId()))
            throw new IllegalArgumentException("Credit card does not belong to the informed account");
        accountService.requireAccess(card.getAccountId());
        return card;
    }

    private static boolean isCardPurchase(Transaction transaction) {
        return transaction.getEntryType() == TransactionEntryType.CARD && transaction.getCreditCard() != null;
    }

    private Category findCategory(UUID categoryId) {
        return categoryService.requireAccessible(categoryId);
    }

    /**
     * A transação vinculada à recorrência segue a categoria da recorrência: alterar no
     * lançamento atualiza o modelo e as ocorrências pendentes (confirmadas são histórico).
     */
    private void syncRecurrenceCategory(Transaction transaction, Category category) {
        Recurrence recurrence = transaction.getRecurrence();
        if (recurrence == null) return;
        recurrence.setCategory(category);
        recurrence.setIncome(category.isIncome());
        recurrenceRepository.save(recurrence);
        repository.updatePendingCategoryByRecurrenceExcept(
                recurrence.getId(), recurrence.getUserId(), category, category.isIncome(), transaction.getId());
    }

    /** Conta contraparte das pernas TRANSFER, para identificar a transferência no extrato. */
    private Map<UUID, String> counterparts(List<Transaction> transactions) {
        List<UUID> transferIds = transactions.stream()
                .filter(transaction -> transaction.getTransferId() != null)
                .map(Transaction::getTransferId)
                .distinct()
                .toList();
        Map<UUID, String> result = new HashMap<>();
        for (UUID transferId : transferIds) {
            List<Transaction> legs = repository.findByTransferId(transferId);
            if (legs.size() != 2) continue;
            for (Transaction leg : legs) {
                for (Transaction other : legs) {
                    if (!leg.getId().equals(other.getId())) result.put(leg.getId(), other.getAccountId());
                }
            }
        }
        return result;
    }

    private TransactionResponseDTO response(Transaction transaction, Map<UUID, String> counterparts) {
        TransactionResponseDTO dto = new TransactionResponseDTO(transaction);
        if (transaction.getTransferId() != null) {
            String counterpart = counterparts == null ? null : counterparts.get(transaction.getId());
            if (counterpart == null) counterpart = resolveCounterpart(transaction);
            dto.setCounterpartAccountId(counterpart);
        }
        return dto;
    }

    private String resolveCounterpart(Transaction transaction) {
        List<Transaction> legs = repository.findByTransferId(transaction.getTransferId());
        if (legs == null) return null;
        return legs.stream()
                .filter(leg -> !leg.getId().equals(transaction.getId()))
                .map(Transaction::getAccountId)
                .findFirst()
                .orElse(null);
    }

    private void requireCategoryDirection(Category category, boolean income) {
        if (category.isIncome() != income)
            throw new IllegalArgumentException(income
                    ? "Income transactions require an income category"
                    : "Expense transactions require an expense category");
    }
}
