// dto/momo/MomoCreateOrderResponse.java
package com.sc.scifunapi.dto.momo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MomoCreateOrderResponse {
    private int resultCode;
    private String message;
    private String orderId;
    private String requestId;
    private String payUrl;      // link mở trình duyệt / webview
    private String deeplink;    // mở thẳng app MoMo
    private String qrCodeUrl;   // ảnh QR
}