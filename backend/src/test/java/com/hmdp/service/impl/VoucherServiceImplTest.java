package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Shop;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherServiceImplTest {

    @Spy
    @InjectMocks
    private VoucherServiceImpl voucherService;

    @Mock
    private VoucherMapper voucherMapper;

    @Mock
    private ISeckillVoucherService seckillVoucherService;

    @Mock
    private IShopService shopService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(voucherService, "baseMapper", voucherMapper);
    }

    @Test
    void queryClaimableVouchers_shouldDelegateToMapperAndReturnOkResult() {
        Voucher seckillVoucher = new Voucher();
        seckillVoucher.setId(1L);
        seckillVoucher.setTitle("秒杀券");
        seckillVoucher.setType(1);
        seckillVoucher.setStock(20);

        Voucher normalVoucher = new Voucher();
        normalVoucher.setId(2L);
        normalVoucher.setTitle("普通券");
        normalVoucher.setType(0);

        List<Voucher> vouchers = List.of(seckillVoucher, normalVoucher);
        when(voucherMapper.queryClaimableVouchers()).thenReturn(vouchers);

        Result result = voucherService.queryClaimableVouchers();

        assertTrue(result.getSuccess());
        assertSame(vouchers, result.getData());
        assertEquals(2, ((List<?>) result.getData()).size());
        verify(voucherMapper).queryClaimableVouchers();
    }

    @Test
    void addVoucher_shouldFailWhenShopDoesNotExist() {
        Voucher voucher = baseVoucher();
        when(shopService.getById(1L)).thenReturn(null);

        Result result = voucherService.addVoucher(voucher);

        assertFalse(result.getSuccess());
        assertEquals("店铺不存在", result.getErrorMsg());
        verify(voucherService, never()).save(any(Voucher.class));
    }

    @Test
    void addVoucher_shouldSaveNormalVoucherWithEnabledStatus() {
        Voucher voucher = baseVoucher();
        when(shopService.getById(1L)).thenReturn(new Shop().setId(1L));
        doAnswer(invocation -> {
            Voucher saved = invocation.getArgument(0);
            saved.setId(31L);
            return true;
        }).when(voucherService).save(any(Voucher.class));

        Result result = voucherService.addVoucher(voucher);

        assertTrue(result.getSuccess());
        assertEquals(31L, result.getData());
        ArgumentCaptor<Voucher> captor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherService).save(captor.capture());
        assertEquals(0, captor.getValue().getType());
        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    void addSeckillVoucher_shouldSaveVoucherSeckillRecordAndRedisStock() {
        Voucher voucher = baseVoucher();
        voucher.setStock(10);
        voucher.setBeginTime(LocalDateTime.now().minusHours(1));
        voucher.setEndTime(LocalDateTime.now().plusHours(1));
        when(shopService.getById(1L)).thenReturn(new Shop().setId(1L));
        when(seckillVoucherService.save(any(SeckillVoucher.class))).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doAnswer(invocation -> {
            Voucher saved = invocation.getArgument(0);
            saved.setId(32L);
            return true;
        }).when(voucherService).save(any(Voucher.class));

        Result result = voucherService.addSeckillVoucher(voucher);

        assertTrue(result.getSuccess());
        assertEquals(32L, result.getData());
        ArgumentCaptor<Voucher> voucherCaptor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherService).save(voucherCaptor.capture());
        assertEquals(1, voucherCaptor.getValue().getType());
        assertEquals(1, voucherCaptor.getValue().getStatus());
        ArgumentCaptor<SeckillVoucher> seckillCaptor = ArgumentCaptor.forClass(SeckillVoucher.class);
        verify(seckillVoucherService).save(seckillCaptor.capture());
        assertEquals(32L, seckillCaptor.getValue().getVoucherId());
        assertEquals(10, seckillCaptor.getValue().getStock());
        verify(valueOperations).set(SECKILL_STOCK_KEY + 32L, "10");
    }

    @Test
    void addSeckillVoucher_shouldFailWhenStockIsInvalid() {
        Voucher voucher = baseVoucher();
        voucher.setStock(0);
        voucher.setBeginTime(LocalDateTime.now().minusHours(1));
        voucher.setEndTime(LocalDateTime.now().plusHours(1));
        when(shopService.getById(1L)).thenReturn(new Shop().setId(1L));

        Result result = voucherService.addSeckillVoucher(voucher);

        assertFalse(result.getSuccess());
        assertEquals("秒杀券库存必须大于0", result.getErrorMsg());
        verify(voucherService, never()).save(any(Voucher.class));
    }

    @Test
    void addSeckillVoucher_shouldFailWhenTimeRangeIsInvalid() {
        Voucher voucher = baseVoucher();
        voucher.setStock(10);
        voucher.setBeginTime(LocalDateTime.now().plusHours(1));
        voucher.setEndTime(LocalDateTime.now().minusHours(1));
        when(shopService.getById(1L)).thenReturn(new Shop().setId(1L));

        Result result = voucherService.addSeckillVoucher(voucher);

        assertFalse(result.getSuccess());
        assertEquals("秒杀券开始时间必须早于结束时间", result.getErrorMsg());
        verify(voucherService, never()).save(any(Voucher.class));
    }

    private Voucher baseVoucher() {
        Voucher voucher = new Voucher();
        voucher.setShopId(1L);
        voucher.setTitle("测试券");
        voucher.setPayValue(8000L);
        voucher.setActualValue(10000L);
        return voucher;
    }
}
