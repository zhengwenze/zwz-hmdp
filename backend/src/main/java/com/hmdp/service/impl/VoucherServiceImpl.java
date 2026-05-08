package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IShopService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.IVoucherService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    private static final int NORMAL_VOUCHER_TYPE = 0;
    private static final int SECKILL_VOUCHER_TYPE = 1;
    private static final int ENABLED_STATUS = 1;

    @Resource
    private IShopService shopService;
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private IVoucherOrderService voucherOrderService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result queryVoucherOfShop(Long shopId) {
        purgeExpiredSeckillVouchers();
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result queryClaimableVouchers() {
        purgeExpiredSeckillVouchers();
        List<Voucher> vouchers = getBaseMapper().queryClaimableVouchers();
        return Result.ok(vouchers);
    }

    @Override
    public Result addVoucher(Voucher voucher) {
        Result validation = validateBaseVoucher(voucher);
        if (!validation.getSuccess()) {
            return validation;
        }
        voucher.setType(NORMAL_VOUCHER_TYPE);
        voucher.setStatus(ENABLED_STATUS);
        boolean saved = save(voucher);
        if (!saved) {
            return Result.fail("创建优惠券失败");
        }
        return Result.ok(voucher.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addSeckillVoucher(Voucher voucher) {
        Result validation = validateBaseVoucher(voucher);
        if (!validation.getSuccess()) {
            return validation;
        }
        if (voucher.getStock() == null || voucher.getStock() <= 0) {
            return Result.fail("秒杀券库存必须大于0");
        }
        if (voucher.getBeginTime() == null || voucher.getEndTime() == null) {
            return Result.fail("秒杀券开始时间和结束时间不能为空");
        }
        if (!voucher.getBeginTime().isBefore(voucher.getEndTime())) {
            return Result.fail("秒杀券开始时间必须早于结束时间");
        }
        voucher.setType(SECKILL_VOUCHER_TYPE);
        voucher.setStatus(ENABLED_STATUS);
        // 保存优惠券
        boolean voucherSaved = save(voucher);
        if (!voucherSaved) {
            return Result.fail("创建秒杀券失败");
        }
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        boolean seckillSaved = seckillVoucherService.save(seckillVoucher);
        if (!seckillSaved) {
            throw new IllegalStateException("创建秒杀券库存记录失败");
        }
        // 保存秒杀库存到 Redis，失败时抛异常以回滚数据库事务。
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
        return Result.ok(voucher.getId());
    }

    private Result validateBaseVoucher(Voucher voucher) {
        if (voucher == null) {
            return Result.fail("优惠券信息不能为空");
        }
        if (voucher.getShopId() == null) {
            return Result.fail("店铺不能为空");
        }
        if (shopService.getById(voucher.getShopId()) == null) {
            return Result.fail("店铺不存在");
        }
        if (StrUtil.isBlank(voucher.getTitle())) {
            return Result.fail("优惠券标题不能为空");
        }
        if (voucher.getPayValue() == null || voucher.getPayValue() <= 0) {
            return Result.fail("支付金额必须大于0");
        }
        if (voucher.getActualValue() == null || voucher.getActualValue() <= 0) {
            return Result.fail("抵扣金额必须大于0");
        }
        return Result.ok();
    }

    private void purgeExpiredSeckillVouchers() {
        List<Long> expiredVoucherIds = getBaseMapper().queryExpiredSeckillVoucherIds();
        if (expiredVoucherIds == null || expiredVoucherIds.isEmpty()) {
            return;
        }

        voucherOrderService.remove(new LambdaQueryWrapper<VoucherOrder>()
                .in(VoucherOrder::getVoucherId, expiredVoucherIds));
        seckillVoucherService.removeBatchByIds(expiredVoucherIds);
        removeBatchByIds(expiredVoucherIds);
        for (Long voucherId : expiredVoucherIds) {
            stringRedisTemplate.delete(SECKILL_STOCK_KEY + voucherId);
            stringRedisTemplate.delete("seckill:order:" + voucherId);
        }
    }
}
