
package com.torodb.torod.mongodb.meta;

import com.google.common.collect.Lists;
import com.torodb.kvdocument.values.ObjectValue;
import com.torodb.torod.core.annotations.DatabaseName;
import com.torodb.torod.core.connection.ToroConnection;
import com.torodb.torod.core.connection.ToroTransaction;
import com.torodb.torod.core.dbWrapper.exceptions.ImplementationDbException;
import com.torodb.torod.core.pojos.NamedToroIndex;
import com.torodb.torod.core.subdocument.ToroDocument;
import com.torodb.torod.mongodb.translator.KVToroDocument;
import com.torodb.torod.mongodb.utils.NamespaceUtil;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *
 */
@Singleton
public class NamespacesMetaCollection extends MetaCollection {

    @Inject
    public NamespacesMetaCollection(@DatabaseName String databaseName) {
        super(databaseName, NamespaceUtil.NAMESPACES_COLLECTION);
    }

    @Override
    public List<ToroDocument> queryAllDocuments(ToroConnection toroConnection) {
        Collection<String> allCollections = toroConnection.getCollections();

        List<ToroDocument> candidates = Lists.newArrayList();
        ToroTransaction transaction = null;
        String databaseName = getDatabaseName();
        try {
            transaction = toroConnection.createTransaction();

            for (String collection : allCollections) {
                String collectionNamespace = databaseName + '.' + collection;

                candidates.add(
                        new KVToroDocument(
                                new ObjectValue.Builder()
                                .putValue("name", collectionNamespace)
                                .build()
                        )
                );

                Collection<? extends NamedToroIndex> indexes
                        = transaction.getIndexes(collection);
                for (NamedToroIndex index : indexes) {
                    candidates.add(
                            new KVToroDocument(
                                    new ObjectValue.Builder()
                                    .putValue("name", collectionNamespace + ".$"
                                            + index.getName())
                                    .build()
                            )
                    );
                }
            }
            candidates.add(
                    new KVToroDocument(
                            new ObjectValue.Builder()
                            .putValue("name", databaseName + ".system.indexes")
                            .build()
                    )
            );

            return candidates;
        }
        catch (ImplementationDbException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        } finally {
            if (transaction != null) {
                transaction.close();
            }
        }
    }

    @Override
    public long count(ToroConnection toroConnection) {
        Collection<String> allCollections = toroConnection.getCollections();

        //count will be our result
        long count = allCollections.size(); //now it counts all standard collections
        ToroTransaction transaction = null;
        try {
            transaction = toroConnection.createTransaction();

            for (String collection : allCollections) {
                count += transaction.getIndexes(collection).size();
            }
            //now count counts all standard collections and its indexes
            count += 1; //we add the 'databaseName'.system.indexes
        }
        catch (ImplementationDbException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        } finally {
            if (transaction != null) {
                transaction.close();
            }
        }
        
        return count;
    }

    @Override
    public boolean isCapped() {
        return false;
    }

    @Override
    public Number getMaxIfCapped() {
        return null;
    }
}
