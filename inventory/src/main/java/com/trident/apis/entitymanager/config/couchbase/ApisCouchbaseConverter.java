package com.trident.apis.entitymanager.config.couchbase;

import com.trident.shared.immigration.repository.RepositoryDecorator;
import org.springframework.data.couchbase.core.convert.MappingCouchbaseConverter;
import org.springframework.data.couchbase.core.mapping.CouchbasePersistentEntity;
import org.springframework.data.couchbase.core.mapping.CouchbasePersistentProperty;
import org.springframework.data.mapping.context.MappingContext;

public class ApisCouchbaseConverter extends MappingCouchbaseConverter {

    public static final String TYPE_KEY_DEFAULT = RepositoryDecorator.TYPE_KEY;

    public ApisCouchbaseConverter(MappingContext<? extends CouchbasePersistentEntity<?>, CouchbasePersistentProperty> mappingContext) {
        super(mappingContext);
        this.typeMapper = new ApisCouchbaseTypeMapper(TYPE_KEY_DEFAULT);
    }

    public ApisCouchbaseConverter(MappingContext<? extends CouchbasePersistentEntity<?>, CouchbasePersistentProperty> mappingContext, String typeKey) {
        super(mappingContext, typeKey);
        this.typeMapper = new ApisCouchbaseTypeMapper(typeKey);
    }
}
