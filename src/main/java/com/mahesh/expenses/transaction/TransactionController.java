package com.mahesh.expenses.transaction;

import com.mahesh.expenses.common.PageResponse;
import com.mahesh.expenses.transaction.TransactionDtos.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final static String IDEMPOTENCY_KEY = "Idempotency-Key";
    private final static String REPLAYED = "Idempotent-Replayed";

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /** GET /api/v1/transactions?page=0&size=20 */
    @GetMapping
    public PageResponse<TransactionResponse> listTransactions(@PageableDefault(size = 20) Pageable pageable) {
        return transactionService.list(pageable);
    }

    @PostMapping("/incomes")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TransactionResponse> createIncomeTransaction(@Valid @RequestBody TransactionRequest transactionRequest,
                                                                       @RequestHeader(name = IDEMPOTENCY_KEY, required = false) String idempotencyKey) {
        return created(transactionService.recordIncome(transactionRequest, idempotencyKey));
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TransactionResponse> createExpenseTransaction(@Valid @RequestBody TransactionRequest transactionRequest,
                                                                        @RequestHeader(name = IDEMPOTENCY_KEY, required = false) String idempotencyKey) {
        return created(transactionService.recordExpense(transactionRequest, idempotencyKey));
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TransactionResponse> createTransferTransaction(@Valid @RequestBody TransferRequest transferRequest,
                                                                         @RequestHeader(name = IDEMPOTENCY_KEY, required = false) String idempotencyKey) {
        return created(transactionService.recordTransfer(transferRequest, idempotencyKey));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable("id") Long transactionId) {
        transactionService.delete(transactionId);
    }

    /** A replay returns the same 201 and body as the original, plus a header saying so. */
    private static ResponseEntity<TransactionResponse> created(TransactionResult result) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(REPLAYED, String.valueOf(result.replayed()))
                .body(result.response());
    }
}
