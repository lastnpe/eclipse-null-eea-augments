package org.lastnpe.examples.hello;

import org.eclipse.jdt.annotation.NonNullByDefault;
import static org.eclipse.jdt.annotation.DefaultLocation.ARRAY_CONTENTS;
import static org.eclipse.jdt.annotation.DefaultLocation.FIELD;
import static org.eclipse.jdt.annotation.DefaultLocation.PARAMETER;
import static org.eclipse.jdt.annotation.DefaultLocation.RETURN_TYPE;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_ARGUMENT;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_BOUND;
import static org.eclipse.jdt.annotation.DefaultLocation.TYPE_PARAMETER;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

@NonNullByDefault({ PARAMETER, RETURN_TYPE, FIELD, TYPE_PARAMETER, TYPE_BOUND, TYPE_ARGUMENT, ARRAY_CONTENTS })
public abstract class CompletionStageWrapper<T> implements CompletionStage<T> {

    @Override
    // "Illegal redefinition of parameter executor, inherited method from CompletionStage<T> does not constrain this parameter"
    // This is wrong, because the CompletionStage EEA annotates Executor as @NonNull
    // Workaround @Nullable Executor makes it disappear, but why is this needed?
    // TODO Is this a JDT bug?!
    public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return CompletableFuture.completedFuture(null);
    }


}
