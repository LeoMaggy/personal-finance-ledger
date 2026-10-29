package com.mahesh.expenses.common;

/**
 * Who is making this request. Services depend on this interface, not on how the user is found.
 * Day 1: a fixed demo user. Day 2: replaced by the user from the JWT, with no service changes.
 */
public interface CurrentUser {
    Long id();
}
