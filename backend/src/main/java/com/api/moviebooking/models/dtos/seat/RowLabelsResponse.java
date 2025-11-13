package com.api.moviebooking.models.dtos.seat;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RowLabelsResponse {

    private int totalRows;
    private List<String> rowLabels;
}
