package com.lms.employee.common.mediator;

import org.springframework.context.ApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Mediator {

    private final ApplicationContext context;

    public Mediator(ApplicationContext context) {
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    public <TCommand, TResult> TResult send(TCommand command) {
        Map<String, ICommandHandler> handlers = context.getBeansOfType(ICommandHandler.class);

        for (ICommandHandler handler : handlers.values()) {
            Class<?>[] typeArgs = GenericTypeResolver.resolveTypeArguments(
                    handler.getClass(), ICommandHandler.class);

            if (typeArgs != null && typeArgs[0].isAssignableFrom(command.getClass())) {
                return (TResult) handler.handle(command);
            }
        }

        throw new IllegalArgumentException(
                "No command handler found for: " + command.getClass().getSimpleName());
    }

    @SuppressWarnings("unchecked")
    public <TQuery, TResult> TResult query(TQuery query) {
        Map<String, IQueryHandler> handlers = context.getBeansOfType(IQueryHandler.class);

        for (IQueryHandler handler : handlers.values()) {
            Class<?>[] typeArgs = GenericTypeResolver.resolveTypeArguments(
                    handler.getClass(), IQueryHandler.class);

            if (typeArgs != null && typeArgs[0].isAssignableFrom(query.getClass())) {
                return (TResult) handler.handle(query);
            }
        }

        throw new IllegalArgumentException(
                "No query handler found for: " + query.getClass().getSimpleName());
    }
}
