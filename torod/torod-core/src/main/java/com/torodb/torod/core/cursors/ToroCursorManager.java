
package com.torodb.torod.core.cursors;

import com.torodb.torod.core.exceptions.CursorNotFoundException;
import com.torodb.torod.core.exceptions.NotAutoclosableCursorException;
import com.torodb.torod.core.executor.SessionExecutor;
import com.torodb.torod.core.language.projection.Projection;
import com.torodb.torod.core.language.querycriteria.QueryCriteria;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 */
public interface ToroCursorManager {

    /**
     * Opens a unlimited cursor that iterates over the given query.
     * <p>
     * @param executor the executor that will execute the open cursor action
     * @param collection
     * @param queryCriteria if null, all documents are returned
     * @param projection    if null, all fields are returned
     * @param numberToSkip
     * @param autoclose
     * @param hasTimeout
     * @return
     * @throws com.torodb.torod.core.exceptions.NotAutoclosableCursorException
     */
    public ToroCursor openUnlimitedCursor(
            @Nonnull SessionExecutor executor,
            @Nonnull String collection,
            @Nullable QueryCriteria queryCriteria,
            @Nullable Projection projection,
            @Nonnegative int numberToSkip,
            boolean autoclose,
            boolean hasTimeout
    ) throws NotAutoclosableCursorException;

    /**
     * Opens a limited cursor that iterates over the given query.
     * <p>
     * @param executor the executor that will execute the open cursor action
     * @param collection
     * @param queryCriteria if null, all documents are returned
     * @param projection    if null, all fields are returned
     * @param numberToSkip
     * @param limit         must be higher than 0
     * @param autoclose
     * @param hasTimeout
     * @return
     */
    public ToroCursor openLimitedCursor(
            @Nonnull SessionExecutor executor,
            @Nonnull String collection,
            @Nullable QueryCriteria queryCriteria,
            @Nullable Projection projection,
            @Nonnegative int numberToSkip,
            int limit,
            boolean autoclose,
            boolean hasTimeout
    );
    
    /**
     * Returns the stored cursor with the given id or throws an exception if
     * there is no cursor with that id.
     * @param cursorId
     * @return 
     * @throws com.torodb.torod.core.exceptions.CursorNotFoundException if 
     *          there are no cursor with the given id
     */
    public ToroCursor lookForCursor(CursorId cursorId) throws CursorNotFoundException ;
}
