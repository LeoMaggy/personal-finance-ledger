package com.mahesh.expenses.recurring;

import java.time.LocalDate;

public enum Frequency {
    WEEKLY, MONTHLY, YEARLY;

    /**
     * The due date after {@code current}. Monthly and yearly bills keep their "anchor" day where the
     * month allows it: rent anchored on the 31st falls on 30 Nov, 28 Feb, then back to 31 Mar.
     * (Plain plusMonths(1) would drift: 31 Jan -> 28 Feb -> 28 Mar -> 28 Apr ... forever.)
     */
    public LocalDate next(LocalDate current, int anchorDay){
        return switch(this){
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> clamp(current.plusMonths(1), anchorDay);
            case YEARLY ->  clamp(current.plusYears(1), anchorDay);
        };
    }

    private static LocalDate clamp(LocalDate date, int anchorDay){
        return date.withDayOfMonth(Math.min(anchorDay, date.lengthOfMonth()));
    }

}
