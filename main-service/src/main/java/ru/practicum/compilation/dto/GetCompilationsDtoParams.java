package ru.practicum.compilation.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetCompilationsDtoParams {

    private boolean pinned;

    @Builder.Default
    @Min(0)
    private Integer from = 0;

    @Builder.Default
    @Min(1)
    private Integer size = 10;
}