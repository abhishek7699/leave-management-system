package com.lms.leave.common.mediator;

public interface IQueryHandler<TQuery, TResult> {
    TResult handle(TQuery query);
}
