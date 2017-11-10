package com.trident.apis.entitymanager.config;

import com.google.common.collect.ImmutableList;
import com.trident.apis.entitymanager.model.*;
import com.trident.apis.entitymanager.model.cvi.CruiseVoyageItinerary;
import com.trident.apis.entitymanager.model.lookup.*;
import com.trident.apis.entitymanager.model.tdrs.TdrsDocument;
import com.trident.apis.entitymanager.model.tdrs.TdrsRule;
import com.trident.apis.entitymanager.repository.*;
import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import com.trident.shared.immigration.repository.RepositoryDecoratorBuilder;
import com.trident.shared.immigration.repository.constraints.Constraint;
import com.trident.shared.immigration.repository.constraints.ConstraintImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Created by dimzul on 11/24/16.
 */
@Configuration
public class RepositoryConfig {

    /* -----===== Repositories =====----- */

    @Bean
    public ApisTemplateCouchbaseRepository apisTemplateCouchbaseRepository() {
        return new ApisTemplateCouchbaseRepository(getApisTemplateConstraints(), ApisTemplate.class, ApisTemplate.APIS_TEMPLATE);
    }

    @Bean
    public RepositoryDecorator<ApisRule> apisRuleUniqueConstraintRepository() {
        return new RepositoryDecoratorBuilder<ApisRule>()
                .setConstraints(getApisRuleConstraints())
                .setEntityClass(ApisRule.class)
                .setEntityType("apisRule")
                .createRepositoryDecorator();
    }

    @Bean
    public ApisSubmissionCouchbaseRepository apisSubmissionCouchbaseRepository() {
        return new ApisSubmissionCouchbaseRepository(ImmutableList.of(), ApisSubmission.class, ApisSubmission.APIS_SUBMISSION);
    }

    private List<Constraint<ApisTemplate>> getApisTemplateConstraints() {
        return ImmutableList.of(
                apisTemplateNameConstraint()
        );
    }

    @Bean
    public CountryCouchbaseRepository getCountyCouchbaseRepository() {
        return new CountryCouchbaseRepository(countryConstraints(), Country.class, Country.COUNTRY);
    }

    @Bean
    public PortCouchbaseRepository portCouchbaseRepository() {
        return new PortCouchbaseRepository(portConstraints(), Port.class, Port.PORT);
    }

    @Bean
    public TdrsDocumentCouchbaseRepository getTdrsDocumentCouchbaseRepository() {
        return new TdrsDocumentCouchbaseRepository(tdrsDocumentConstraints(), TdrsDocument.class, TdrsDocument.TDRS_DOCUMENT);
    }


    @Bean
    public TdrsRuleCouchbaseRepository getTdrsRuleCouchbaseRepository() {
        return new TdrsRuleCouchbaseRepository(tdrsRuleConstraints(), TdrsRule.class, TdrsRule.TDRS_RULE);
    }

    @Bean
    public RepositoryDecorator<CodeDictionary> codeDictionaryRepository() {
        return new RepositoryDecoratorBuilder<CodeDictionary>()
                .setConstraints(ImmutableList.of())
                .setEntityClass(CodeDictionary.class)
                .setEntityType(CodeDictionary.CODE_DICTIONARY)
                .createRepositoryDecorator();
    }

    @Bean
    public RepositoryDecorator<HistoryEntity> historyRepository() {
        return new RepositoryDecoratorBuilder<HistoryEntity>()
                .setConstraints(ImmutableList.of())
                .setEntityClass(HistoryEntity.class)
                .setEntityType(HistoryEntity.TYPE)
                .createRepositoryDecorator();
    }

    @Bean("documentContentCouchbaseRepository")
    public RepositoryDecorator<DocumentContent> documentContentCouchbaseRepository() {
        RepositoryDecoratorBuilder<DocumentContent> builder = new RepositoryDecoratorBuilder<>();
        return builder.setEntityClass(DocumentContent.class)
                .setEntityType("documentContent")
                .createRepositoryDecorator();
    }

    @Bean
    public TridentToPortTranslationCouchbaseRepository toPortTranslationCouchbaseRepository() {
        return new TridentToPortTranslationCouchbaseRepository(ImmutableList.of(),
                TridentToPortTranslation.class,
                TridentToPortTranslation.TYPE);
    }

    @Bean
    public ShipCouchbaseRepository shipCouchbaseRepository() {
        return new ShipCouchbaseRepository(ImmutableList.of(), ShipEntity.class, ShipEntity.SHIP);
    }

    @Bean
    public CruiseVoyageItineraryCouchbaseRepository cruiseVoyageItineraryCouchbaseRepository() {
        return new CruiseVoyageItineraryCouchbaseRepository(
                ImmutableList.of(),
                CruiseVoyageItinerary.class,
                CruiseVoyageItinerary.CRUISE_VOYAGE_ITINERARY);
    }


    @Bean
    public CompanyCouchbaseRepository companyCouchbaseRepository() {
        return new CompanyCouchbaseRepository(ImmutableList.of(), CompanyEntity.class, CompanyEntity.COMPANY);
    }

    /* ----===== Constraints =====----- */

    private List<Constraint<Country>> countryConstraints() {
        return ImmutableList.of(
                countryCodeConstraint(),
                countryNameConstraint()
        );
    }

    private List<Constraint<Port>> portConstraints() {
        return ImmutableList.of(
                portCodeConstraint()
                // commented until we know if port name should be unique
//                        ,
//                        new RepositoryDecorator.ConstraintImpl<>(
//                                portNameLookupRepository,
//                                port -> "port-name-" + port.getName(),
//                                (id, field) -> new PortNameLookup(field, id),
//                                field -> portNameLookupRepository.findOneByObjectIdEquals(field))
        );
    }

    private List<Constraint<TdrsDocument>> tdrsDocumentConstraints() {
        return ImmutableList.of(tdrsDocumentNameConstraint());
    }

    private List<Constraint<TdrsRule>> tdrsRuleConstraints() {
        return ImmutableList.of(tdrsRuleNameConstraint());
    }

    @Bean
    public ConstraintImpl<ApisTemplate, ApisTemplateNameLookup> apisTemplateNameConstraint() {
        return new ConstraintImpl<>(
                apisTemplate -> RepositoryDecorator.getPrefix() + "apisTemplate-name-" + apisTemplate.getName(),
                ApisTemplateNameLookup.class,
                ApisTemplateNameLookup.APIS_TEMPLATE_NAME_LOOKUP
        );
    }

    private List<Constraint<ApisRule>> getApisRuleConstraints() {
        return ImmutableList.of(
                apisRuleNameConstraint()
        );
    }

    @Bean
    public ConstraintImpl<ApisRule, ApisRuleNameLookup> apisRuleNameConstraint() {
        return new ConstraintImpl<ApisRule, ApisRuleNameLookup>(
                apisRule -> RepositoryDecorator.getPrefix() + "apisRule-name-" + apisRule.getName(),
                ApisRuleNameLookup.class,
                ApisRuleNameLookup.APIS_RULE_NAME_LOOKUP
        );
    }

    @Bean
    public ConstraintImpl<Country, CountryNameLookup> countryNameConstraint() {
        return new ConstraintImpl<>(
                country -> RepositoryDecorator.getPrefix() + "country-name-" + country.getName(),
                CountryNameLookup.class,
                CountryNameLookup.COUNTRY_NAME_LOOKUP);
    }

    @Bean
    public ConstraintImpl<Country, CountryCodeLookup> countryCodeConstraint() {
        return new ConstraintImpl<>(
                country -> RepositoryDecorator.getPrefix() + "country-code-" + country.getCode(),
                CountryCodeLookup.class,
                CountryCodeLookup.COUNTRY_CODE_LOOKUP);
    }

    @Bean
    public ConstraintImpl<Port, PortCodeLookup> portCodeConstraint() {
        return new ConstraintImpl<>(
                port -> RepositoryDecorator.getPrefix() + "port-code-" + port.getCode(),
                PortCodeLookup.class,
                PortCodeLookup.DOCUMENT_NAME);
    }

    @Bean
    public ConstraintImpl<TdrsDocument, GenericLookup> tdrsDocumentNameConstraint() {
        return new ConstraintImpl<>(
                tdrsDocument -> RepositoryDecorator.getPrefix() + "tdrs-document-" + tdrsDocument.getName(),
                GenericLookup.class,
                TdrsDocument.TDRS_DOCUMENT_NAME_LOOKUP
        );
    }

    @Bean
    public ConstraintImpl<TdrsRule, GenericLookup> tdrsRuleNameConstraint() {
        return new ConstraintImpl<>(
                tdrsRule -> RepositoryDecorator.getPrefix() + "tdrs-rule-" + tdrsRule.getName(),
                GenericLookup.class,
                TdrsRule.TDRS_RULE_NAME_LOOKUP);
    }

}
