package com.mahesh.expenses.transaction;

import com.mahesh.expenses.common.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SortWhitelist {
    /**
     * Clients sort by public names ("date", "amount"), never by entity paths. Without this,
     * ?sort=user.passwordHash would be passed straight into the query; an unknown name would be a 500.
     */
    public static final Map<String, String> ALLOWED = Map.of(
            "date", "transactionDate",
            "amount", "amount"
    );

    private SortWhitelist() {
    }

    static Pageable apply(Pageable request){
        List<Sort.Order> orders = new ArrayList<>();
        for(Sort.Order order : request.getSort()){
            String property = ALLOWED.get(order.getProperty());
            if(property == null){
                throw new BadRequestException("Can't sort by '%s'. Allowed '%s': ".formatted(order.getProperty(), ALLOWED.keySet()));
            }
            orders.add(new Sort.Order(order.getDirection(), property));
        }
        if(orders.isEmpty()){
            orders.add(Sort.Order.desc("transactionDate")); // default sort
        }
        orders.add(Sort.Order.desc("id")); // tie-breaker for same date
        return PageRequest.of(request.getPageNumber(), request.getPageSize(), Sort.by(orders));
    }
}
