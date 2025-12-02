package com.sc.scifunapi.dto.zalo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)   // để Zalo trả thêm field nào cũng không bị lỗi
public class ZlpCreateOrderResponse {

    @JsonProperty("return_code")
    private int returnCode;

    @JsonProperty("return_message")
    private String returnMessage;

    @JsonProperty("sub_return_code")
    private Integer subReturnCode;

    @JsonProperty("sub_return_message")
    private String subReturnMessage;

    @JsonProperty("order_url")
    private String orderUrl;

    @JsonProperty("qr_code")
    private String qrCode;

    @JsonProperty("zp_trans_token")
    private String zpTransToken;

    @JsonProperty("zp_trans_id")
    private String zpTransId;

    @JsonProperty("mac")
    private String mac;

    // Cái này không phải field trong JSON, mình tự set trong service
    private String appTransId;
}
