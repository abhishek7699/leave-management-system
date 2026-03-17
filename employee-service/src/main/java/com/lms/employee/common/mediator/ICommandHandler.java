package com.lms.employee.common.mediator;

public interface ICommandHandler<TCommand, TResult> {
    TResult handle(TCommand command);
}
