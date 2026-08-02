package com.cryptotrading.dto.releasenote;

import lombok.Data;
import java.util.List;

/**
 * 릴리즈 노트 일괄 삭제 요청 DTO
 */
@Data
public class BulkDeleteRequest {
    private List<Long> ids;
}