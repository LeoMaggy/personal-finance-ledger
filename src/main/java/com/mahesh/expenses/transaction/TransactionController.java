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
    public ResponseEntity<TransactionResponse> createIncomeTransaction(@Valid @RequestBody TransactionRequest transactionRequest){
        TransactionResponse response = transactionService.recordIncome(transactionRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TransactionResponse> createExpenseTransaction(@Valid @RequestBody TransactionRequest transactionRequest){
        TransactionResponse response = transactionService.recordExpense(transactionRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TransactionResponse> createTransferTransaction(@Valid @RequestBody TransferRequest transferRequest){
        TransactionResponse response = transactionService.recordTransfer(transferRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable("id") Long transactionId) {
        transactionService.delete(transactionId);
    }
}
