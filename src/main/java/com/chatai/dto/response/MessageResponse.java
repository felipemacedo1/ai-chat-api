package com.chatai.dto.response;

import com.chatai.entity.Message;
import com.chatai.entity.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    
    private String id;
    private String content;
    private MessageRole role;
    private LocalDateTime createdAt;
    
    public static MessageResponse fromEntity(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .role(message.getRole())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
