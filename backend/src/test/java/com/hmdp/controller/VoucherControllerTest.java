package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.service.IVoucherService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VoucherController.class)
@ContextConfiguration(classes = VoucherController.class)
class VoucherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IVoucherService voucherService;

    @Test
    void addVoucher_shouldBindRequestBodyAndReturnVoucherId() throws Exception {
        when(voucherService.save(any(Voucher.class))).thenAnswer(invocation -> {
            Voucher voucher = invocation.getArgument(0);
            voucher.setId(21L);
            return true;
        });

        mockMvc.perform(post("/voucher")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"shopId\":1,\"title\":\"普通券\",\"payValue\":8000,\"actualValue\":10000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(21));

        ArgumentCaptor<Voucher> captor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherService).save(captor.capture());
        assertEquals(1L, captor.getValue().getShopId());
        assertEquals("普通券", captor.getValue().getTitle());
    }

    @Test
    void addSeckillVoucher_shouldBindRequestBodyAndReturnVoucherId() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            Voucher voucher = invocation.getArgument(0);
            voucher.setId(22L);
            return null;
        }).when(voucherService).addSeckillVoucher(any(Voucher.class));

        mockMvc.perform(post("/voucher/seckill")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"shopId\":1,\"title\":\"秒杀券\",\"stock\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(22));

        ArgumentCaptor<Voucher> captor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherService).addSeckillVoucher(captor.capture());
        assertEquals(1L, captor.getValue().getShopId());
        assertEquals("秒杀券", captor.getValue().getTitle());
        assertEquals(10, captor.getValue().getStock());
    }

    @Test
    void queryVoucherOfShop_shouldBindShopIdAndReturnServiceResult() throws Exception {
        Voucher voucher = new Voucher();
        voucher.setId(23L);
        voucher.setTitle("店铺券");
        when(voucherService.queryVoucherOfShop(1L)).thenReturn(Result.ok(List.of(voucher)));

        mockMvc.perform(get("/voucher/list/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(23))
                .andExpect(jsonPath("$.data[0].title").value("店铺券"));

        verify(voucherService).queryVoucherOfShop(1L);
    }
}
