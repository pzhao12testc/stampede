
package com.torodb.torod.mongodb.commands.impl.general;

import com.eightkdata.mongowp.ErrorCode;
import com.eightkdata.mongowp.OpTime;
import com.eightkdata.mongowp.exceptions.CommandFailed;
import com.eightkdata.mongowp.exceptions.MongoException;
import com.eightkdata.mongowp.exceptions.UnknownErrorException;
import com.eightkdata.mongowp.server.api.Command;
import com.eightkdata.mongowp.server.api.CommandImplementation;
import com.eightkdata.mongowp.server.api.CommandRequest;
import com.eightkdata.mongowp.server.api.CommandResult;
import com.eightkdata.mongowp.server.api.impl.SimpleWriteOpResult;
import com.eightkdata.mongowp.server.api.impl.WriteCommandResult;
import com.eightkdata.mongowp.mongoserver.api.safe.library.v3m0.commands.general.DeleteCommand.DeleteArgument;
import com.eightkdata.mongowp.mongoserver.api.safe.library.v3m0.commands.general.DeleteCommand.DeleteStatement;
import com.eightkdata.mongowp.server.callback.WriteOpResult;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.torodb.torod.core.WriteFailMode;
import com.torodb.torod.core.connection.DeleteResponse;
import com.torodb.torod.core.connection.ToroConnection;
import com.torodb.torod.core.connection.ToroTransaction;
import com.torodb.torod.core.connection.TransactionMetainfo;
import com.torodb.torod.core.dbWrapper.exceptions.ImplementationDbException;
import com.torodb.torod.core.language.operations.DeleteOperation;
import com.torodb.torod.core.language.querycriteria.QueryCriteria;
import com.torodb.torod.mongodb.RequestContext;
import com.torodb.torod.mongodb.commands.AbstractToroRetryCommandImplementation;
import com.torodb.torod.mongodb.commands.WriteConcernToWriteFailModeFunction;
import com.torodb.torod.mongodb.translator.QueryCriteriaTranslator;
import java.util.List;
import javax.inject.Inject;

/**
 *
 */
public class DeleteImplementation extends AbstractToroRetryCommandImplementation<DeleteArgument, Long> {

    private final WriteConcernToWriteFailModeFunction toWriteFailModeFunction;
    private final QueryCriteriaTranslator queryCriteriaTranslator;
    private final DeleteStatementToDeleteOperation toDeleteOperation;

    @Inject
    public DeleteImplementation(
            WriteConcernToWriteFailModeFunction toWriteFailModeFunction,
            QueryCriteriaTranslator queryCriteriaTranslator) {
        this.toWriteFailModeFunction = toWriteFailModeFunction;
        this.queryCriteriaTranslator = queryCriteriaTranslator;
        this.toDeleteOperation = new DeleteStatementToDeleteOperation();
    }
    
    @Override
    public CommandResult<Long> tryApply(
            Command<? super DeleteArgument, ? super Long> command,
            CommandRequest<DeleteArgument> req) throws MongoException {

        DeleteArgument arg = req.getCommandArgument();

        RequestContext context = RequestContext.getFrom(req);
        String supportedDatabase = context.getSupportedDatabase();

        if (!supportedDatabase.equals(req.getDatabase())) {
            throw new CommandFailed(
                    command.getCommandName(),
                    "Database '"+req.getDatabase()+"' is not supported. "
                            + "Only '" + supportedDatabase +"' is supported");
        }

		ToroConnection connection = context.getToroConnection();

        try (ToroTransaction transaction
                = connection.createTransaction(TransactionMetainfo.NOT_READ_ONLY)) {
            WriteFailMode writeFailMode = toWriteFailModeFunction.apply(arg.getWriteConcern());

            List<DeleteOperation> deletes = Lists.newArrayList(
                    Iterables.transform(arg.getStatements(), toDeleteOperation)
            );

            ListenableFuture<DeleteResponse> deleteFuture = transaction.delete(
                    arg.getCollection(),
                    deletes,
                    writeFailMode
            );
            ListenableFuture<?> commitFuture = transaction.commit();
            DeleteResponse response = Futures.get(
                    deleteFuture, UnknownErrorException.class);
            Futures.get(commitFuture, UnknownErrorException.class);
            OpTime optime = context.getOptimeClock().tick();
            WriteOpResult writeOpResult;
            if (response.isSuccess()) {
                writeOpResult = new SimpleWriteOpResult(ErrorCode.OK, null, null, optime);
            }
            else {
                ErrorCode errorCode = ErrorCode.COMMAND_FAILED;
                String errMsg = "Something went wrong";
                writeOpResult = new SimpleWriteOpResult(errorCode, errMsg, null, null, optime);
            }
            return new WriteCommandResult<>(response.getDeleted(), writeOpResult);
        } catch (ImplementationDbException ex) {
            throw new UnknownErrorException(ex);
        }
    }

    private class DeleteStatementToDeleteOperation implements Function<DeleteStatement, DeleteOperation> {

        @Override
        public DeleteOperation apply(DeleteStatement input) {
            if (input == null) {
                return null;
            }
            
            QueryCriteria filter = queryCriteriaTranslator.translate(input.getQuery());

            return new DeleteOperation(filter, input.isJustOne());
        }

    }
}
