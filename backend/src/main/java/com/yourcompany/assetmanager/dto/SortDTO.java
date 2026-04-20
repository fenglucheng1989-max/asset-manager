package com.yourcompany.assetmanager.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SortDTO {

    @NotEmpty(message = "Sorted IDs list cannot be empty")
    private List<Long> sortedIds;
}
