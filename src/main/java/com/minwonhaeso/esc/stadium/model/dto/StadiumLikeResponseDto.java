package com.minwonhaeso.esc.stadium.model.dto;

import com.minwonhaeso.esc.stadium.model.entity.StadiumLike;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StadiumLikeResponseDto {
    private Long id;

    private String name;

    private String address;

    private Double starAvg;


    public static StadiumLikeResponseDto fromEntity(StadiumLike stadiumLike) {
        return StadiumLikeResponseDto.builder()
                .id(stadiumLike.getId())
                .name(stadiumLike.getStadium().getName())
                .address(stadiumLike.getStadium().getAddress())
                .starAvg(stadiumLike.getStadium().getStarAvg())
                .build();
    }
}