package com.minwonhaeso.esc.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    StadiumNotFound(HttpStatus.BAD_REQUEST, "일치하는 체육관 정보가 존재하지 않습니다.");

    private final HttpStatus statusCode;
    private final String errorMessage;
}
