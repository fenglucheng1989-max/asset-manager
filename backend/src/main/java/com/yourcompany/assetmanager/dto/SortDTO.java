package com.yourcompany.assetmanager.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SortDTO {

    @NotEmpty(message = "排序账户列表不能为空")
    private List<Long> sortedIds;
}
