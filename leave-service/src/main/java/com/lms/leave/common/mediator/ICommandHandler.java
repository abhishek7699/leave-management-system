package com.lms.leave.common.mediator;

public interface ICommandHandler<TCommand, TResult> {
    TResult handle(TCommand command);
}
