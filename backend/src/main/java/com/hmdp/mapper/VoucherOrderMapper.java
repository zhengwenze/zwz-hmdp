package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.dto.MyVoucherDTO;
import com.hmdp.entity.VoucherOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {

    List<MyVoucherDTO> queryMyVouchers(@Param("userId") Long userId);
}
