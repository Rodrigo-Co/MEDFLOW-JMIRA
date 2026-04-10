package com.medflow.dto;
import lombok.Data;
@Data
public class Pre2FAResponse {
    private String  sessionToken;
    private String  maskedEmail;
    private boolean requires2FA = true;
    public Pre2FAResponse(String sessionToken, String maskedEmail) {
        this.sessionToken = sessionToken;
        this.maskedEmail  = maskedEmail;
    }
}
