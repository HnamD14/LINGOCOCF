package com.example.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitPaymentRequest {

    @NotBlank(message = "Mã đơn hàng không được trống")
    @Pattern(regexp = "^LC\\d{7,10}$", message = "Mã đơn hàng không hợp lệ (dạng LC1234567)")
    @Schema(example = "LC1234567")
    private String orderCode;

    @NotBlank(message = "Mã giao dịch không được trống")
    @Size(min = 4, max = 30, message = "Mã giao dịch 4–30 ký tự")
    @Schema(example = "MBVCB123456789")
    private String transactionCode;

    @Size(max = 200, message = "Ghi chú tối đa 200 ký tự")
    @Schema(example = "Nâng cấp PRO 3 tháng")
    private String note;
}
