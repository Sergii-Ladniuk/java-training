package com.trident.apis.entitymanager.model;

import com.trident.shared.immigration.dto.apis.ApisRuleDto;
import com.trident.shared.immigration.dto.apis.method.ApisSubmissionMethod;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.couchbase.core.mapping.Document;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@ToString @Getter @Setter
@Document
public class ApisRule extends CouchbaseEntityWithId {

    public static final String APIS_RULE = "apisRule";
    @NotNull
    private String name;
    @NotNull
    private List<String> apisPortIds;
    @NotNull
    private List<String> apisTemplateIds;
    @NotNull
    @Valid
    private ApisSubmissionMethod submissionMethod;
    private boolean shouldReviewKnowledge;
    @NotNull(message = "TridentToPortTranslation's ID should not be null")
    private String tridentToPortTranslationId;
    @NotNull
    @ApiModelProperty(value = "Is rule applicable to arrival or departure")
    private ApisRuleDto.ApisDirection direction;
    @NotNull
    @ApiModelProperty(value = "Is rule applicable to electronic submissions (APIS) or paper submissions (HARDCOPY port paper documents)")
    private ApisRuleDto.ApisRuleType ruleType;

    public ApisRule() {
        super(APIS_RULE);
    }

    public boolean isShouldReviewKnowledge() {
        return shouldReviewKnowledge;
    }
}
