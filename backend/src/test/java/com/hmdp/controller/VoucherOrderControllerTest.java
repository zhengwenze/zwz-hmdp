package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoucherOrderController.class)
@ContextConfiguration(classes = VoucherOrderController.class)
class VoucherOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IVoucherOrderService voucherOrderService;

    @Test
    void seckillVoucher_shouldBindVoucherIdAndReturnServiceResult() throws Exception {
        when(voucherOrderService.seckillVoucher(10L)).thenReturn(Result.ok(1001L));

        mockMvc.perform(post("/voucher-order/seckill/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(1001));

        verify(voucherOrderService).seckillVoucher(10L);
    }
}
