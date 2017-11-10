package com.trident.apis.entitymanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.trident.shared.immigration.dto.apis.*;
import com.trident.shared.immigration.dto.apis.cvi.CruiseVoyageItineraryDto;
import com.trident.shared.immigration.dto.apis.knowledge.ApisKnowledge;
import com.trident.shared.immigration.dto.apis.knowledge.ApisKnowledgeValidationError;
import com.trident.shared.immigration.dto.apis.knowledge.NoticeDetails;
import com.trident.shared.immigration.dto.apis.method.ApisSubmissionMethod;
import com.trident.shared.immigration.dto.apis.translation.TridentToPortTranslationDto;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.couchbase.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Document
@Setter @Getter @ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApisSubmission extends CouchbaseEntityWithId {

    public static final String APIS_SUBMISSION = "apisSubmission";

    @NotNull
    private String ruleId;
    @NotNull
    private String ruleName;
    @NotNull
    private String portId;
    @NotNull
    private String portName;
    private ApisKnowledge knowledge;
    private TridentToPortTranslationDto tridentToPortTranslation;
    private Map<String, Object> knowledgeTranslated;
    @NotNull
    private List<ApisTemplateWithExtra> templates;
    private List<ApisDocument> documents;
    @NotNull
    private long startDate;
    @NotNull
    private long statusDate;
    private List<String> errors;
    @ApiModelProperty(value = "APIS Knowledge validation errors", required = false)
    private List<ApisKnowledgeValidationError> validationErrors;
    @NotNull
    private ApisSubmissionStatus status;
    @NotNull(message = "Apis Submission Method is mandatory")
    private ApisSubmissionMethod submissionMethod;
    private String trackingReference;
    private boolean shouldReviewKnowledge;
    @ApiModelProperty(value = "Is rule applicable to arrival or departure")
    private ApisRuleDto.ApisDirection direction;
    @ApiModelProperty(value = "Is rule applicable to electronic submissions (APIS) or paper submissions (HARDCOPY port paper documents)")
    private ApisRuleDto.ApisRuleType ruleType;
    @ApiModelProperty(value = "Notice transaction type: INITIAL or UPDATE")
    private NoticeDetails.NoticeTransactionType noticeTransactionType;
    @NotNull(message = "DepartedFromPort flag is mandatory")
    @ApiModelProperty(value = "Whether ship still in port or departed from it")
    private Boolean departedFromPort;
    private CruiseVoyageItineraryDto cruiseVoyageItineraryDto;
    private String cruiseVoyageItineraryEntryId;
    private String previousSubmissionId;
    private String nextSubmissionId;

    public ApisSubmission() {
        super(APIS_SUBMISSION);
    }

    @JsonIgnore
    @Override
    public String getName() {
        return ruleName;
    }
}
