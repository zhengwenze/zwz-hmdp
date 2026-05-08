package com.hmdp.controller;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShopController.class)
@ContextConfiguration(classes = ShopController.class)
class ShopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IShopService shopService;

    @Test
    void queryShopById_shouldReturnShop() throws Exception {
        Shop shop = new Shop();
        shop.setId(1L);
        shop.setName("测试商铺");

        when(shopService.queryById(1L)).thenReturn(Result.ok(shop));

        mockMvc.perform(get("/shop/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("测试商铺"));

        verify(shopService).queryById(1L);
    }

    @Test
    void saveShop_shouldBindRequestBodyAndReturnOk() throws Exception {
        mockMvc.perform(post("/shop")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"新增商铺\",\"typeId\":2,\"address\":\"测试地址\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(shopService).save(any(Shop.class));
    }

    @Test
    void updateShop_shouldBindRequestBodyAndReturnServiceResult() throws Exception {
        when(shopService.update(any(Shop.class))).thenReturn(Result.ok("updated"));

        mockMvc.perform(put("/shop")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"name\":\"更新商铺\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("updated"));

        verify(shopService).update(any(Shop.class));
    }

    @Test
    void queryShopByType_shouldBindRequestParamsAndReturnServiceResult() throws Exception {
        when(shopService.queryShopByType(1, 2, 121.1, 31.2)).thenReturn(Result.ok("shops"));

        mockMvc.perform(get("/shop/of/type")
                .param("typeId", "1")
                .param("current", "2")
                .param("x", "121.1")
                .param("y", "31.2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("shops"));

        verify(shopService).queryShopByType(1, 2, 121.1, 31.2);
    }

    @Test
    void queryShopByName_shouldReturnPagedRecords() throws Exception {
        QueryChainWrapper<Shop> queryWrapper = org.mockito.Mockito.mock(QueryChainWrapper.class);
        Shop shop = new Shop();
        shop.setId(3L);
        shop.setName("咖啡店");
        Page<Shop> page = new Page<>();
        page.setRecords(List.of(shop));

        when(shopService.query()).thenReturn(queryWrapper);
        when(queryWrapper.like(eq(true), eq("name"), eq("咖啡"))).thenReturn(queryWrapper);
        when(queryWrapper.page(any(Page.class))).thenReturn(page);

        mockMvc.perform(get("/shop/of/name")
                .param("name", "咖啡")
                .param("current", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(3))
                .andExpect(jsonPath("$.data[0].name").value("咖啡店"));

        verify(shopService).query();
    }
}
