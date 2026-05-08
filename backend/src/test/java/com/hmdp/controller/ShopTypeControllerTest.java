package com.hmdp.controller;

import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.service.IShopTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShopTypeController.class)
@ContextConfiguration(classes = ShopTypeController.class)
class ShopTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IShopTypeService typeService;

    @Test
    void queryTypeList_shouldReturnServiceResult() throws Exception {
        ShopType shopType = new ShopType();
        shopType.setId(1L);
        shopType.setName("美食");
        when(typeService.getTypeList()).thenReturn(Result.ok(List.of(shopType)));

        mockMvc.perform(get("/shop-type/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("美食"));

        verify(typeService).getTypeList();
    }
}
