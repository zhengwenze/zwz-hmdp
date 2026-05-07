package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.Blog;

// 声明这是一个 MyBatis-Plus 映射接口。
// 继承 BaseMapper<Blog> 后，自动拥有：
// selectById(id)
// selectList(wrapper)
// insert(entity)
// updateById(entity)
// deleteById(id)
// selectPage(page, wrapper)
public interface BlogMapper extends BaseMapper<Blog> {

}
