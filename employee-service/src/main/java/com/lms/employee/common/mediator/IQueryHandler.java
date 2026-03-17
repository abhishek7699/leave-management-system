package com.lms.employee.common.mediator;

public interface IQueryHandler<TQuery, TResult> {
    TResult handle(TQuery query);
}
