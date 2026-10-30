package com.mahesh.expenses.audit;

import com.mahesh.expenses.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    /** GET /api/v1/audit?page=0&size=20 */
    @GetMapping
    public PageResponse<AuditEntryResponse> myAuditTrail(@PageableDefault(size = 20) Pageable pageable) {
        return service.myAuditTrail(pageable);
    }
}
