package com.trident.apis.entitymanager;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import com.google.common.collect.ImmutableList;
import com.trident.apis.entitymanager.model.*;
import com.trident.apis.entitymanager.model.cvi.CruiseVoyageItinerary;
import com.trident.apis.entitymanager.model.tdrs.TdrsDocument;
import com.trident.apis.entitymanager.model.tdrs.TdrsRule;
import com.trident.apis.entitymanager.repository.*;
import com.trident.apis.entitymanager.service.CountryMapper;
import com.trident.shared.immigration.dto.apis.ApisRuleDto;
import com.trident.shared.immigration.dto.apis.ApisSubmissionStatus;
import com.trident.shared.immigration.dto.apis.DocumentOutputType;
import com.trident.shared.immigration.dto.apis.translation.TridentToPortFieldTranslation;
import com.trident.shared.immigration.dto.apis.cvi.CruiseVoyageItineraryDto;
import com.trident.shared.immigration.dto.apis.cvi.CviPortEntry;
import com.trident.shared.immigration.dto.apis.knowledge.ApisKnowledge;
import com.trident.shared.immigration.dto.apis.knowledge.legal.Address;
import com.trident.shared.immigration.dto.apis.knowledge.legal.Company;
import com.trident.shared.immigration.dto.apis.knowledge.ship.*;
import com.trident.shared.immigration.dto.apis.method.ApisSubmissionMethod;
import com.trident.shared.immigration.dto.apis.method.ApisSubmissionMethodCredentials;
import com.trident.shared.immigration.dto.apis.method.ApisSubmissionMethodCredentialsType;
import com.trident.shared.immigration.dto.apis.method.ApisSubmissionMethodType;
import com.trident.shared.immigration.dto.apis.port.PortDto;
import com.trident.shared.immigration.dto.tdrs.*;
import com.trident.shared.immigration.dto.tdrs.recommendation.*;
import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.repository.RepositoryDecorator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by dimzul on 11/24/16.
 */
@Component
public class MockDataProvider {

    public static final String TDRS_DOCUMENT_NAME = "tdrs document name";
    public static final String TDRS_DOCUMENT_CODE = "tdrs_document_code";
    public static final String TDRS_DESCRIPTION = "tdrs document description";
    public static final String TDRS_RULE_NAME = "tdrs rule name";

    public enum MockType {
        APIS_TEMPLATE, APIS_RULE, APIS_SUBMISSION, COUNTRY, PORT, TDRS_DOCUMENT, TDRS_RULE, CODE_DICTIONARY,
        COMPANY_ENTITY, SHIP_ENTITY, TRIDENT_TO_PORT_TRANSLATION, CVI, DOC_CONTENT, HISTORY
    }

    public static final String DEFAULT_USER_ID = "default_user_id";

    private static final String TEMPLATE_ID_PREFIX = "templateId",
            COMPANY_ID_PREFIX = "companyId",
            SHIP_ID_PREFIX = "shipId",
            RULE_ID_PREFIX = "ruleId",
            SUBMISSION_ID_PREFIX = "submissionId",
            COUNTRY_ID_PREFIX = "countryId",
            PORT_ID_PREFIX = "portId",
            TDRS_DOCUMENT_ID_PREFIX = "tdrsDocumentId",
            TDRS_RULE_ID_PREFIX = "tdrsRuleId",
            CODE_DICTIOINARY_ID_PREFIX = "dive:lookup:code",
            TRIDENT_TO_PORT_TRANSLATATION_PREFIX = "tridetToPortTranslation",
            CVI_PREFIX = "cvi",
            DOC_CONTENT_PREFIX = "docContentId";

    @Autowired
    private CouchbaseTemplate couchbaseTemplate;
    @Autowired
    private RepositoryDecorator<ApisTemplate> templateRepositoryDecorator;
    @Autowired
    private RepositoryDecorator<ApisRule> ruleRepositoryDecorator;
    @Autowired
    private ApisSubmissionCouchbaseRepository submissionRepositoryDecorator;
    @Autowired
    private CountryCouchbaseRepository countryRepositoryDecorator;
    @Autowired
    private CompanyCouchbaseRepository companyEntityRepositoryDecorator;
    @Autowired
    private ShipCouchbaseRepository shipEntityRepositoryDecorator;
    @Autowired
    private PortCouchbaseRepository portRepositoryDecorator;
    @Autowired
    private TdrsDocumentCouchbaseRepository tdrsDocumentRepository;
    @Autowired
    private TdrsRuleCouchbaseRepository tdrsRuleCouchbaseRepository;
    @Autowired
    private RepositoryDecorator<CodeDictionary> codeDictionaryRepositoryDecorator;
    @Autowired
    private TridentToPortTranslationCouchbaseRepository tridentToPortTranslationRepositoryDecorator;
    @Autowired
    private CruiseVoyageItineraryCouchbaseRepository cviRepositoryDecorator;
    @Autowired
    private RepositoryDecorator<DocumentContent> documentContentRepositoryDecorator;
    @Autowired
    private CountryMapper countryMapper;
    @Autowired
    @Qualifier("bucketWithRetry")
    private Bucket bucket;

    private static ApisSubmissionMethod[] submissionMethods = new ApisSubmissionMethod[]{
        createFtpSubmissionMethod(),
        createHttpSubmissionMethod()
    };

    public static String getRuleId(int id) {
        return RULE_ID_PREFIX + id;
    }

    public static String getTemplateId(int id) {
        return TEMPLATE_ID_PREFIX + id;
    }

    public static String getCompanyId(int id) {
        return COMPANY_ID_PREFIX + id;
    }

    public static String getShipId(int id) {
        return SHIP_ID_PREFIX + id;
    }

    public static String getSubmissionId(int id) {
        return SUBMISSION_ID_PREFIX + id;
    }

    public static String getCountryId(int id) {
        return COUNTRY_ID_PREFIX + id;
    }

    public static String getPortId(int id) {
        return PORT_ID_PREFIX + id;
    }

    public static String getTdrsDocumentId(int id) {
        return TDRS_DOCUMENT_ID_PREFIX + id;
    }

    public static String getTdrsRuleId(int id) {
        return TDRS_RULE_ID_PREFIX + id;
    }

    public static String getCodeDictioinaryId(int id) {
        return CODE_DICTIOINARY_ID_PREFIX + id;
    }

    public static String getTridentToPortTranslatationId(int id) {
        return TRIDENT_TO_PORT_TRANSLATATION_PREFIX + id;
    }

    public static String getCviId(int id) {
        return CVI_PREFIX + id;
    }

    public static String getDocumentContentId(int id) {
        return DOC_CONTENT_PREFIX + id;
    }

    public void cleanDb(MockType type) {
        clearDbAndHistory();
    }

    public void insertMock(MockType type, int count) {
        List mocks;
        switch (type) {
            case APIS_TEMPLATE:
                mocks = createMockTemplates(count);
                templateRepositoryDecorator.bulkSaveWithHistory(mocks, DEFAULT_USER_ID);
                break;
            case APIS_RULE:
                mocks = createMockApisRules(count);
                ruleRepositoryDecorator.bulkSaveWithHistory(mocks, DEFAULT_USER_ID);
                break;
            case APIS_SUBMISSION:
                mocks = createMockSubmissions(count);
                submissionRepositoryDecorator.bulkSaveWithHistory(mocks, DEFAULT_USER_ID);
                break;
            case COUNTRY:
                mocks = createMockCountries(count);
                countryRepositoryDecorator.bulkSaveWithHistory(mocks, DEFAULT_USER_ID);
                break;
            case PORT:
                mocks = createMockPorts(count);
                portRepositoryDecorator.bulkSaveWithHistory(mocks, DEFAULT_USER_ID);
                break;
            case TDRS_DOCUMENT:
                mocks = createTdrsDocumentMocks(count);
                tdrsDocumentRepository.bulkSaveWithHistory(mocks, DEFAULT_USER_ID);
                break;
            case TDRS_RULE:
                mocks = createMockTdrsRules(count);
                tdrsRuleCouchbaseRepository.bulkSaveWithHistory(mocks, DEFAULT_USER_ID);
                break;
            case CODE_DICTIONARY:
                mocks = createCodeDictionaries(count);
                codeDictionaryRepositoryDecorator.bulkSaveWithHistory(mocks, DEFAULT_USER_ID);
                break;
            case TRIDENT_TO_PORT_TRANSLATION:
                mocks = createTridentToPortTranslationList(count);
                tridentToPortTranslationRepositoryDecorator.bulkSaveWithHistory(mocks, DEFAULT_USER_ID);
                break;
            case COMPANY_ENTITY:
                companyEntityRepositoryDecorator.bulkSaveWithHistory(createCompanyList(count), DEFAULT_USER_ID);
                break;
            case SHIP_ENTITY:
                shipEntityRepositoryDecorator.bulkSaveWithHistory(createShipList(count), DEFAULT_USER_ID);
                break;
            case CVI:
                cviRepositoryDecorator.bulkSaveWithHistory(createMockCvi(count), DEFAULT_USER_ID);
                break;
            case DOC_CONTENT:
                documentContentRepositoryDecorator.bulkSaveWithHistory(createMockDocContent(count), DEFAULT_USER_ID);
                break;
            default:
                throw new IllegalArgumentException("Not supported type");
        }
    }

    public ApisSubmissionMethod getHttpSubmissionMethod() {
        return submissionMethods[1];
    }

    public TdrsDocument createTdrsDocument(int id) {
        TdrsDocument tdrsDocument = new TdrsDocument();
        tdrsDocument.setId(getTdrsDocumentId(id));
        tdrsDocument.setDescription(TDRS_DESCRIPTION + id);
        tdrsDocument.setName(TDRS_DOCUMENT_NAME + id);
        tdrsDocument.setCode(TDRS_DOCUMENT_CODE + id);
        tdrsDocument.setApisDocumentType("apis");
        tdrsDocument.setTdrsAttributes(new TdrsAttributeDto[]{
            TdrsAttributeDto.builder()
            .name("attr1")
            .type(TdrsAttributeType.INTEGER)
            .subType(TdrsAttributeSubType.OTHER)
            .apisFieldType("dummy")
            .mandatory(false)
            .build()
        });
        return tdrsDocument;
    }

    public static Country createCountry(int id) {
        Country country = new Country();
        country.setName("countryName" + id);
        String code = (Integer.toString(id) + "XX").substring(0, 2);
        country.setCode(code);
        country.setId(getCountryId(id));
        return country;
    }

    public static Port createPort(int id) {
        Port port = new Port();
        port.setId(getPortId(id));
        port.setName("portName" + id);
        port.setCode("portCode" + id);
        port.setCountryId(getCountryId(id));
        port.setUnCode("unCode" + id);
        port.setPolarCode("polarCode" + id);
        return port;
    }

    public static PortDto createPortDto(int i) {
        PortDto port = new PortDto();
        port.setId(getPortId(i));
        port.setName("portName" + i);
        port.setState("state" + i);
        port.setCode("portCode" + i);
        port.setCity("cityId" + i);
        port.setCountryId(getCountryId(i));
        port.setUnCode("unCode" + i);
        port.setPolarCode("polarCode" + i);
        port.setDescription("Description" + i);
        return port;
    }

    public TdrsRule createTdrsRule(int id, LogicNodeType type) {
        List<TdrsDocument> documents = prepareTdrsDocumentsForRule(id);
        Set<RecommendationNode> documentNodes = tdrsRulesToDocumentNode(documents);
        LogicNode logicNode = new LogicNode(type, documentNodes);

        TdrsRule tdrsRule = newTdrsRule(id);
        tdrsRule.setRecommendation(logicNode);
        return tdrsRule;
    }

    public TdrsRuleDto createTdrsRuleDto(int id, LogicNodeType type) {
        List<TdrsDocument> documents = prepareTdrsDocumentsForRule(id);
        Set<RecommendationNode> documentNodes = tdrsDocumentsToDocumentNodeDto(documents);
        LogicNode logicNode = new LogicNode(type, documentNodes);

        TdrsRule tdrsRule = newTdrsRule(id);
        TdrsRuleDto tdrsRuleDto = tdrsRule.toDto();
        tdrsRuleDto.setRecommendation(logicNode);
        if (tdrsRule.getCitizenshipCountryIds() != null) {
            tdrsRuleDto.setCitizenshipCountries(countryMapper.idsToDtos(tdrsRule.getCitizenshipCountryIds()));
        }
        if (tdrsRule.getDestinationCountryId() != null) {
            tdrsRuleDto.setDestinationCountry(countryMapper.idToDto(tdrsRule.getDestinationCountryId()));
        }
        return tdrsRuleDto;
    }

    public CodeDictionary createCodeDictionary(int id) {
        CodeDictionary codeDictionary = new CodeDictionary();
        codeDictionary.setId(getCodeDictioinaryId(id));
        Set<String> codes = new HashSet<>();
        codes.add(String.valueOf(id * 10 + 1));
        codes.add(String.valueOf(id * 10 + 2));
        codes.add(String.valueOf(id * 10 + 3));
        codeDictionary.setCodes(codes);
        return codeDictionary;
    }

    private Set<RecommendationNode> tdrsRulesToDocumentNode(List<TdrsDocument> documents) {
        return documents.stream().map(doc -> new DocumentNode(doc.getId())).collect(Collectors.toSet());
    }

    private Set<RecommendationNode> tdrsDocumentsToDocumentNodeDto(List<TdrsDocument> documents) {
        return documents.stream().map(doc -> new DocumentNodeDto(tdrsDocumentToDto(doc))).collect(Collectors.toSet());
    }

    private List<TdrsDocument> prepareTdrsDocumentsForRule(int id) {
        List<TdrsDocument> documents;
        if (id == 0) {
            documents = new ArrayList<>();
            documents.add(createTdrsDocument(0));
        } else {
            documents = IntStream.of(id, id * 10).mapToObj(this::createTdrsDocument).collect(Collectors.toList());
        }
        tdrsDocumentRepository.bulkSaveWithHistory(documents, DEFAULT_USER_ID);
        return documents;
    }

    public static ApisSubmission createApisSubmission(int id) {
        ApisSubmission apisSubmission = new ApisSubmission();
        apisSubmission.setId(getSubmissionId(id));
        apisSubmission.setPortId(getPortId(id));
        apisSubmission.setPortName("portName" + id);
        apisSubmission.setRuleId(getRuleId(id));
        apisSubmission.setRuleName("ruleName" + id);
        apisSubmission.setKnowledge(new ApisKnowledge());
        apisSubmission.setTemplates(ImmutableList.of());
        apisSubmission.setDocuments(ImmutableList.of());
        apisSubmission.setStartDate(System.currentTimeMillis());
        apisSubmission.setStatusDate(System.currentTimeMillis());
        apisSubmission.setStatus(ApisSubmissionStatus.KNOWLEDGE_PENDING);
        apisSubmission.setSubmissionMethod(submissionMethods[0]);
        return apisSubmission;
    }

    public static com.trident.shared.immigration.dto.apis.ApisSubmission createApisSubmissionDto(int id) {
        com.trident.shared.immigration.dto.apis.ApisSubmission apisSubmissionDto = new com.trident.shared.immigration.dto.apis.ApisSubmission();
        apisSubmissionDto.setId(getSubmissionId(id));
        apisSubmissionDto.setPortId(getPortId(id));
        apisSubmissionDto.setPortName("portName" + id);
        apisSubmissionDto.setRuleId(getRuleId(id));
        apisSubmissionDto.setRuleName("ruleName" + id);
        apisSubmissionDto.setKnowledge(new ApisKnowledge());
        apisSubmissionDto.setTemplates(ImmutableList.of());
        apisSubmissionDto.setDocuments(ImmutableList.of());
        apisSubmissionDto.setStartDate(System.currentTimeMillis());
        apisSubmissionDto.setStatusDate(System.currentTimeMillis());
        apisSubmissionDto.setStatus(ApisSubmissionStatus.KNOWLEDGE_PENDING);
        apisSubmissionDto.setSubmissionMethod(submissionMethods[0]);
        return apisSubmissionDto;
    }

    public TridentToPortTranslation createTridentToPortTranslation(int id) {
        TridentToPortFieldTranslation fieldTranslation = new TridentToPortFieldTranslation();
        fieldTranslation.setFieldPath("notice.vessel.idType");
        fieldTranslation.setMandatory(false);
        fieldTranslation.setTridentToApisMapping(new HashMap<String, String>() {
            {
                put("tridentType" + id, "portType" + id);
            }
        });
        fieldTranslation.setMaxChars(20 + id);

        TridentToPortTranslation translation = new TridentToPortTranslation();
        translation.setName("translationName" + id);
        translation.setId(getTridentToPortTranslatationId(id));
        translation.setFieldsTranslation(new HashMap<String, TridentToPortFieldTranslation>() {
            {
                put("notice.vessel.idType" + id, fieldTranslation);
            }
        });
        return translation;
    }

    private CompanyEntity createCompany(int i) {
        return CompanyEntity.of(
                getCompanyId(i),
                Company
                        .builder()
                        .name("company name" + i)
                        .address(new Address("USA", "CA", "San Jose", "04123", "154 1av, apt. 43"))
                        .brandCode("brandCode" + i)
                        .phone("phone" + i)
                        .build());
    }

    public static ShipEntity createShipEntity(int i) {
        return ShipEntity.of(
                getShipId(i),
                createShip(i)
        );
    }

    public static Ship createShip(int i) {
        return Ship.builder()
                .shipLegal(ShipLegal
                        .builder()
                        .nationalityCountryId(getCountryId(i))
                        .ownerId(getCompanyId(i))
                        .agentId(getCompanyId(i))
                        .build())
                .shipTechnicalSpec(ShipTechnicalSpec
                        .builder()
                        .shipBuildInfo(ShipBuildInfo
                                .builder()
                                .manufacturerId(getCompanyId(i))
                                .builtAtCountryId(getCountryId(i))
                                .build())
                        .build())
                .shipIdentity(ShipIdentity
                        .builder()
                        .name(getShipName(i))
                        .build())
                .brandCode("brandCode" + i)
                .shipTridentCode("tridentCode" + i)
                .build();
    }

    public static ShipDto createShipDto(int i) {
        ShipDto shipDto = new ShipDto();
        shipDto.setId(getShipId(i));
        shipDto.setShip(createShip(i));
        return shipDto;
    }

    private CruiseVoyageItinerary createCvi(int i) {
        CruiseVoyageItinerary cvi = new CruiseVoyageItinerary();
        cvi.setId(getCviId(i));
        cvi.setBrandCode("brandCode" + i);
        cvi.setShipCode("shipCode" + i);
        cvi.setVoyageNumber("voyageNumber" + i);
        cvi.setDescription("description" + i);
        cvi.setNotes("note" + i);
        cvi.setStartDate(System.currentTimeMillis());
        cvi.setEndDate(System.currentTimeMillis());
        cvi.setShipId(getShipId(i));
        cvi.setCviPortEntries(IntStream.range(0, 3)
                .mapToObj(MockDataProvider::createCviPortEntry)
                .collect(Collectors.toList()));
        return cvi;
    }

    public static CruiseVoyageItineraryDto createCviDto(int i) {
        CruiseVoyageItineraryDto cviDto = new CruiseVoyageItineraryDto();
        cviDto.setId(getCviId(i));
        cviDto.setBrandCode("brndCd" + i);
        cviDto.setShipCode("shpCd" + i);
        cviDto.setVoyageNumber("voyageNumber" + i);
        cviDto.setDescription("description" + i);
        cviDto.setNotes("note" + i);
        cviDto.setStartDate(1000);
        cviDto.setEndDate(2000);
        cviDto.setShip(createShipDto(1));
        cviDto.setShipId(cviDto.getShip().getId());
        cviDto.setCviPortEntries(IntStream.range(0, 3)
                .mapToObj(MockDataProvider::createCviPortEntryDto)
                .collect(Collectors.toList()));
        return cviDto;
    }

    private static CviPortEntry createCviPortEntry(int i) {
        CviPortEntry cviPortEntry = new CviPortEntry();
        cviPortEntry.setPortId(PORT_ID_PREFIX + i);
        cviPortEntry.setVoyageTypeArrival("voyageTypeArrival" + i);
        cviPortEntry.setVoyageTypeDeparture("voyageTypeDeparture" + i);
        cviPortEntry.setArrivalDate(System.currentTimeMillis() + 10000 * i);
        cviPortEntry.setDepartureDate(System.currentTimeMillis() + 20000 * i);
        cviPortEntry.setApisSubmissionIds(
                IntStream.range(0, 3)
                        .mapToObj(MockDataProvider::getSubmissionId)
                        .collect(Collectors.toList()));
        return cviPortEntry;
    }

    public static CviPortEntry createCviPortEntryDto(int i) {
        CviPortEntry cviPortEntry = new CviPortEntry();
        cviPortEntry.setPort(createPortDto(i));
        cviPortEntry.setPortId(cviPortEntry.getPort().getId());
        cviPortEntry.setVoyageTypeArrival("voyageTypeArrival" + i);
        cviPortEntry.setVoyageTypeDeparture("voyageTypeDeparture" + i);
        cviPortEntry.setArrivalDate(System.currentTimeMillis());
        cviPortEntry.setDepartureDate(System.currentTimeMillis());
        cviPortEntry.setApisSubmissions(createApisSubmissionDtos(3));
        return cviPortEntry;
    }

    public static DocumentContent createDocContent(int id) {
        DocumentContent documentContent = new DocumentContent();
        documentContent.setId(getDocumentContentId(id));
        documentContent.setContent("Content" + id);
        return documentContent;
    }

    public static String getShipName(int i) {
        return "ship name" + i;
    }

    private TdrsRule newTdrsRule(int id) {
        TdrsRule tdrsRule = new TdrsRule();
        tdrsRule.setId(TDRS_RULE_ID_PREFIX + id);
        tdrsRule.setName(TDRS_RULE_NAME + id);
        tdrsRule.setCondition("");
        tdrsRule.setDestinationCountryId(getCountryId(id));
        tdrsRule.setCitizenshipCountryIds(new String[]{getCountryId(id), getCountryId(id + 1)});
        tdrsRule.setOrdinal(id);
        return tdrsRule;
    }

    private static ApisSubmissionMethod createHttpSubmissionMethod() {
        ApisSubmissionMethod method = new ApisSubmissionMethod();
        method.setType(ApisSubmissionMethodType.HTTP);
        method.setAddress("http://some.port.gov");
        method.setCredentials(anonymous());
        return method;
    }

    private static ApisSubmissionMethodCredentials anonymous() {
        ApisSubmissionMethodCredentials apisSubmissionMethodCredentials = new ApisSubmissionMethodCredentials();
        apisSubmissionMethodCredentials.setType(ApisSubmissionMethodCredentialsType.ANONYMOUS);
        return apisSubmissionMethodCredentials;
    }

    private static ApisSubmissionMethod createFtpSubmissionMethod() {
        ApisSubmissionMethod method = new ApisSubmissionMethod();
        method.setType(ApisSubmissionMethodType.FTP);
        method.setAddress("ftp://some.port.gov");
        method.setCredentials(credsWithPassword());
        return method;
    }

    private static ApisSubmissionMethodCredentials credsWithPassword() {
        ApisSubmissionMethodCredentials credentials = new ApisSubmissionMethodCredentials();
        credentials.setType(ApisSubmissionMethodCredentialsType.USERNAME_PASSWORD);
        credentials.setUsername("dummy_user");
        credentials.setPassword("dummy_password");
        return credentials;
    }

    private List<ApisTemplate> createMockTemplates(int count) {
        List<ApisTemplate> mocks = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            ApisTemplate template = new ApisTemplate();
            template.setId(getTemplateId(i));
            template.setName("templateName" + i);
            template.setOriginalFileName("dummy.xml");
            template.setDescription("templateDescription" + i);
            template.setContent("content" + i);
            template.setType(DocumentOutputType.EXCEL);
            mocks.add(template);
        }
        return mocks;
    }

    private List<ApisRule> createMockApisRules(int count) {
        List<ApisRule> mocks = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            ApisRule apisRule = new ApisRule();
            apisRule.setId(getRuleId(i));
            apisRule.setName("ruleName" + i);
            apisRule.setApisPortIds(ImmutableList.of(getPortId(i), getPortId(i + 1)));
            apisRule.setApisTemplateIds(ImmutableList.of(getTemplateId(i), getTemplateId(i + 1)));
            apisRule.setTridentToPortTranslationId(getTridentToPortTranslatationId(i));
            if (i < submissionMethods.length) {
                apisRule.setSubmissionMethod(submissionMethods[i]);
            }
            apisRule.setDirection(ApisRuleDto.ApisDirection.ARRIVAL);
            apisRule.setRuleType(ApisRuleDto.ApisRuleType.APIS);
            mocks.add(apisRule);
        }
        return mocks;
    }

    private static List<ApisSubmission> createMockSubmissions(int count) {
        List<ApisSubmission> mocks = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            ApisSubmission apisSubmission = createApisSubmission(i);
            mocks.add(apisSubmission);
        }
        return mocks;
    }

    public static List<com.trident.shared.immigration.dto.apis.ApisSubmission> createApisSubmissionDtos(int count) {
        List<com.trident.shared.immigration.dto.apis.ApisSubmission> mocks = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            com.trident.shared.immigration.dto.apis.ApisSubmission apisSubmissionDto = createApisSubmissionDto(i);
            mocks.add(apisSubmissionDto);
        }
        return mocks;
    }

    private List<Country> createMockCountries(int count) {
        List<Country> countries = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            countries.add(createCountry(i));
        }
        return countries;
    }

    private List<Port> createMockPorts(int count) {
        List<Port> ports = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            ports.add(createPort(i));
        }
        return ports;
    }

    private List<TdrsDocument> createTdrsDocumentMocks(int count) {
        return IntStream.range(0, count).mapToObj(this::createTdrsDocument).collect(Collectors.toList());
    }

    private List<TdrsRule> createMockTdrsRules(int count) {
        List<TdrsRule> tdrsRules = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            LogicNodeType nodeType = i == 0 ? LogicNodeType.EMPTY : i % 2 == 0 ? LogicNodeType.AND : LogicNodeType.OR;
            tdrsRules.add(createTdrsRule(i, nodeType));
        }
        return tdrsRules;
    }

    private List<CruiseVoyageItinerary> createMockCvi(int count) {
        return IntStream.range(0, count).mapToObj(this::createCvi).collect(Collectors.toList());
    }

    private List<CodeDictionary> createCodeDictionaries(int count) {
        return IntStream.range(0, count).mapToObj(this::createCodeDictionary).collect(Collectors.toList());
    }

    private List<TridentToPortTranslation> createTridentToPortTranslationList(int count) {
        return IntStream.range(0, count).mapToObj(this::createTridentToPortTranslation).collect(Collectors.toList());
    }

    private List<CompanyEntity> createCompanyList(int count) {
        return IntStream.range(0, count).mapToObj(this::createCompany).collect(Collectors.toList());
    }

    private List<ShipEntity> createShipList(int count) {
        return IntStream.range(0, count).mapToObj(MockDataProvider::createShipEntity).collect(Collectors.toList());
    }

    private List<DocumentContent> createMockDocContent(int count) {
        return IntStream.range(0, count).mapToObj(MockDataProvider::createDocContent).collect(Collectors.toList());
    }

    private TdrsDocumentDto tdrsDocumentToDto(TdrsDocument tdrsDocument) {
        TdrsDocumentDto tdrsDocumentDto = new TdrsDocumentDto();
        tdrsDocumentDto.setId(tdrsDocument.getId());
        tdrsDocumentDto.setDescription(tdrsDocument.getDescription());
        tdrsDocumentDto.setName(tdrsDocument.getName());
        tdrsDocumentDto.setPolarCode(tdrsDocument.getPolarCode());
        return tdrsDocumentDto;
    }

    private void clearDbAndHistory() {
        String deleteQuery = String.format("DELETE FROM %s WHERE __type like '%s%%'",
                couchbaseTemplate.getCouchbaseBucket().name(),
                RepositoryDecorator.getPrefix());
        couchbaseTemplate.queryN1QL(
                N1qlQuery.simple(deleteQuery, N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS)));
    }
}
