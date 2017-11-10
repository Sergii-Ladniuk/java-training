package com.trident.apis.entitymanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trident.shared.immigration.model.CouchbaseEntityWithId;
import lombok.Data;

import java.util.UUID;

@Data
public class DocumentContent extends CouchbaseEntityWithId {

    public static final String DOCUMENT_CONTENT = "documentContent";

    private String content;
    private String fileName;
    private Boolean base64Encoded;

    public DocumentContent() {
        super(DOCUMENT_CONTENT);
    }

    private DocumentContent(String content, String id) {
        this();
        this.content = content;
        this.setId(id);
    }

    public static DocumentContent fromRichFormat(String content, String filename) {
        DocumentContent documentContent = new DocumentContent(content, UUID.randomUUID().toString());
        documentContent.base64Encoded = true;
        documentContent.fileName = filename;
        return documentContent;
    }

    public static DocumentContent fromPlainText(String content) {
        DocumentContent documentContent = new DocumentContent(content, UUID.randomUUID().toString());
        documentContent.base64Encoded = false;
        return documentContent;
    }

    @JsonIgnore
    @Override
    public String getName() {
        return "N/A";
    }
}
