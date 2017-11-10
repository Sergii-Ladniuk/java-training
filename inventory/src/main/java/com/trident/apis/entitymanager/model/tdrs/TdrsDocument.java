package com.trident.apis.entitymanager.model.tdrs;

import com.trident.shared.immigration.dto.tdrs.TdrsAttributeDto;
import com.trident.shared.immigration.dto.tdrs.TdrsDocumentDto;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static com.trident.apis.entitymanager.model.ModelConstants.LOOKUP;
import static com.trident.apis.entitymanager.model.ModelConstants.NAME;

@Getter @Setter @ToString
public class TdrsDocument extends CouchbaseEntityWithId {

    public static final String TDRS_DOCUMENT = "tdrsDocument";
    public static final String TDRS_DOCUMENT_NAME_LOOKUP = TDRS_DOCUMENT + NAME + LOOKUP;

    @NotNull(message = "Document name is mandatory.")
    private String name;
    @NotNull(message = "Document code is mandatory.")
    @Pattern(regexp = "[\\w]{1,255}", message = "Code should be not empty and contain less than 255 word characters, numbers, underscores (a-zA-Z1-9_).")
    private String code;
    @Size(min = 1, max = 255, message = "Polar code should be not empty and less than 255 characters.")
    private String polarCode;
    @Size(min = 1, max = 255, message = "TDRS Document attributes list should be non-empty and contain less than 255 attributes.")
    @NotNull(message = "TDRS Document attributes list is mandatory.")
    private TdrsAttributeDto[] tdrsAttributes;
    @Size(min = 1, max = 255, message = "APIS document type should be not empty and less than 255 characters.")
    @NotNull(message = "APIS document type is mandatory.")
    private String apisDocumentType;
    private String description;

    public TdrsDocument() {
        super(TDRS_DOCUMENT);
    }

    public TdrsDocumentDto toDto() {
        TdrsDocumentDto dto = new TdrsDocumentDto();
        dto.setId(getId());
        dto.set__etag(get__etag());
        dto.set__type(get__type());
        dto.setName(getName());
        dto.setDescription(getDescription());
        dto.setTdrsAttributes(getTdrsAttributes());
        dto.setApisDocumentType(getApisDocumentType());
        dto.setPolarCode(getPolarCode());
        dto.setCode(getCode());

        return dto;
    }
}
