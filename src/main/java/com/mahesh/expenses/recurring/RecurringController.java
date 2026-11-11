package com.mahesh.expenses.recurring;

import com.mahesh.expenses.recurring.RecurringDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recurring")
public class RecurringController {

    private final RecurringService service;

    public RecurringController(RecurringService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecurringResponse> list() {
        return service.listRecurringExpenses();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringResponse create(@Valid @RequestBody CreateRecurringRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/pay")
    public PayRecurringResponse pay(@PathVariable("id") Long id, @Valid @RequestBody PayRecurringRequest request) {
        return service.pay(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop(@PathVariable("id") Long id) {
        service.stop(id);
    }
}