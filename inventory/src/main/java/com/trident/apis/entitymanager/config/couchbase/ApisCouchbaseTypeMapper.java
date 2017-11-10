package com.trident.apis.entitymanager.config.couchbase;

import com.google.common.collect.Lists;
import com.trident.apis.entitymanager.model.*;
import com.trident.apis.entitymanager.model.lookup.*;
import com.trident.apis.entitymanager.model.tdrs.TdrsRule;
import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import org.springframework.data.convert.ConfigurableTypeInformationMapper;
import org.springframework.data.convert.DefaultTypeMapper;
import org.springframework.data.couchbase.core.convert.CouchbaseTypeMapper;
import org.springframework.data.couchbase.core.convert.DefaultCouchbaseTypeMapper;
import org.springframework.data.couchbase.core.mapping.CouchbaseDocument;

import java.util.HashMap;
import java.util.Map;

public class ApisCouchbaseTypeMapper extends DefaultTypeMapper<CouchbaseDocument> implements CouchbaseTypeMapper {

    private String typeKey;

    public ApisCouchbaseTypeMapper(String typeKey) {
        super(new DefaultCouchbaseTypeMapper.CouchbaseDocumentTypeAliasAccessor(typeKey),
                Lists.newArrayList(new ConfigurableTypeInformationMapper(getTypeMapping())));
        this.typeKey = typeKey;
    }

    private static Map<Class<?>, String> getTypeMapping() {
        Map<Class<?>, String> typeMapping = new HashMap<>();
        typeMapping.put(ApisTemplate.class, RepositoryDecorator.getPrefix() + "apisTemplate");
        typeMapping.put(ApisTemplateNameLookup.class, RepositoryDecorator.getPrefix() + "apisTemplateNameLookup");
        typeMapping.put(ApisRule.class, RepositoryDecorator.getPrefix() + "apisRule");
        typeMapping.put(ApisRuleNameLookup.class, RepositoryDecorator.getPrefix() + "apisRuleNameLookup");
        typeMapping.put(ApisSubmission.class, RepositoryDecorator.getPrefix() + "apisSubmission");
        typeMapping.put(Country.class, RepositoryDecorator.getPrefix() + "country");
        typeMapping.put(Port.class, RepositoryDecorator.getPrefix() + "port");
        typeMapping.put(CountryCodeLookup.class, RepositoryDecorator.getPrefix() + "countryCodeLookup");
        typeMapping.put(CountryNameLookup.class, RepositoryDecorator.getPrefix() + "countryNameLookup");
        typeMapping.put(PortCodeLookup.class, RepositoryDecorator.getPrefix() + "portCodeLookup");
        typeMapping.put(PortNameLookup.class, RepositoryDecorator.getPrefix() + "portNameLookup");
        typeMapping.put(HistoryEntity.class, RepositoryDecorator.getPrefix() + HistoryEntity.TYPE);
        typeMapping.put(TdrsRule.class, RepositoryDecorator.getPrefix() + "tdrsRule");
        return typeMapping;
    }

    @Override
    public String getTypeKey() {
        return typeKey;
    }
}
