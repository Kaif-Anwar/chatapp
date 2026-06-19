package com.kaif.chatapp.chat.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ReadReceiptDTO {
    private Long conversationId;
    private Long messageId;
    private Long readerId;
    private String status;
}
