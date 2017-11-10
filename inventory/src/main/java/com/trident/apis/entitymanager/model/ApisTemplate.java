package com.trident.apis.entitymanager.model;

import com.trident.shared.immigration.dto.apis.ApisValidation;
import com.trident.shared.immigration.dto.apis.DocumentOutputType;
import com.trident.shared.immigration.dto.apis.template.PdfDocumentMapping;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.couchbase.core.mapping.Document;

import java.util.Set;

@Document
@Getter @Setter
public class ApisTemplate extends CouchbaseEntityWithId {

    public static final String APIS_TEMPLATE = "apisTemplate";

    private String name;
    private String content;
    private String contentId;
    private String description;
    private DocumentOutputType type;
    private ApisValidation apisValidation;
    private String originalFileName;
    private String xsdFileName;
    private PdfDocumentMapping pdfDocumentMapping;
    private Set<String> customKnowledgeProviderNames;
    private String notes;
    private String noteId;
    private String noteFileName;
    private Boolean richFormatNotes;
    private String processor;

    public ApisTemplate() {
        super(APIS_TEMPLATE);
    }
}
