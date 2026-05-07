package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.BlogComments;

// 继承 BaseMapper<BlogComments> 后，自动拥有：
// selectById(id)
// selectList(wrapper)
// insert(entity)
// updateById(entity)
// deleteById(id)
// selectPage(page, wrapper)
public interface BlogCommentsMapper extends BaseMapper<BlogComments> {

}
