package com.yourcompany.assetmanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yourcompany.assetmanager.entity.LegalDocument;
import com.yourcompany.assetmanager.exception.BusinessException;
import com.yourcompany.assetmanager.mapper.LegalDocumentMapper;
import com.yourcompany.assetmanager.vo.ApiResponse;
import com.yourcompany.assetmanager.vo.LegalDocumentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/legal-documents")
@RequiredArgsConstructor
public class LegalDocumentController {

    private final LegalDocumentMapper legalDocumentMapper;

    @GetMapping("/latest")
    public ApiResponse<Map<String, LegalDocumentVO>> latest() {
        return ApiResponse.success(Map.of(
                "terms", toVO(requireLatest("TERMS")),
                "privacy", toVO(requireLatest("PRIVACY"))
        ));
    }

    @GetMapping("/latest/{type}")
    public ApiResponse<LegalDocumentVO> latestByType(@PathVariable String type) {
        return ApiResponse.success(toVO(requireLatest(normalizeDocType(type))));
    }

    private LegalDocument requireLatest(String docType) {
        LegalDocument document = legalDocumentMapper.selectOne(new LambdaQueryWrapper<LegalDocument>()
                .eq(LegalDocument::getDocType, docType)
                .eq(LegalDocument::getEnabled, true)
                .orderByDesc(LegalDocument::getEffectiveDate)
                .orderByDesc(LegalDocument::getId)
                .last("LIMIT 1"));
        if (document == null) {
            throw new BusinessException(404, "法律文档未配置");
        }
        return document;
    }

    private String normalizeDocType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase();
        return switch (normalized) {
            case "TERMS", "PRIVACY" -> normalized;
            default -> throw new BusinessException(400, "不支持的文档类型");
        };
    }

    private LegalDocumentVO toVO(LegalDocument document) {
        return LegalDocumentVO.builder()
                .id(document.getId())
                .docType(document.getDocType())
                .title(document.getTitle())
                .version(document.getVersion())
                .effectiveDate(document.getEffectiveDate())
                .content(document.getContent())
                .enabled(document.getEnabled())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
