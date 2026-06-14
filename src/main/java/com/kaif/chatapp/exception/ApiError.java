package com.kaif.chatapp.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized API error response.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ApiError {

    private int status;
    private String message;
    private List<String> details;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
