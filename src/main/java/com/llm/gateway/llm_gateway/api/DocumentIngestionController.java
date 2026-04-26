package com.llm.gateway.llm_gateway.api;

import com.llm.gateway.llm_gateway.domain.model.DocumentChunk;
import com.llm.gateway.llm_gateway.domain.model.DocumentIngestionRequest;
import com.llm.gateway.llm_gateway.domain.service.DocumentIngestionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentIngestionController {

    private DocumentIngestionService documentIngestionService;

    public DocumentIngestionController(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }

    @PostMapping("/ingest")
    String ingestDocument(@RequestBody DocumentIngestionRequest request){
        try{
            documentIngestionService.ingestDocument(request.content(), request.allowedGroups());
            return "Stored document successfully";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
