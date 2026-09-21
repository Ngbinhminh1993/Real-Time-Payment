package com.paymentplatform.paymentservice.adapter.in.rest;

import com.paymentplatform.paymentservice.application.PaymentService;
import com.paymentplatform.paymentservice.domain.exception.PaymentNotFoundException;
import com.paymentplatform.paymentservice.domain.model.Money;
import com.paymentplatform.paymentservice.domain.model.Payment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test: only the controller and MVC machinery are loaded;
 * the application service is mocked. Validates HTTP semantics and the error
 * model without starting a full context or database.
 */
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void createsPayment() throws Exception {
        Payment payment = new Payment("ACC-1", "ACC-2", new Money(new BigDecimal("500.00"), "USD"));
        when(paymentService.createPayment(any())).thenReturn(payment);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccountId": "ACC-1",
                                  "destinationAccountId": "ACC-2",
                                  "amount": 500.00,
                                  "currency": "USD"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(payment.paymentId()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void rejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccountId": "",
                                  "destinationAccountId": "ACC-2",
                                  "amount": 0,
                                  "currency": "US"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void returnsNotFound() throws Exception {
        when(paymentService.getPayment("PAY-404"))
                .thenThrow(new PaymentNotFoundException("PAY-404"));

        mockMvc.perform(get("/api/payments/PAY-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
    }
}
