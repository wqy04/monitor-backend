// package com.example.monitor;

// import com.example.monitor.entity.User;
// import com.example.monitor.mapper.UserMapper;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;

// import java.time.LocalDateTime;

// @SpringBootTest
// public class MysqlConnectTest {
//     // 注入Mapper
//     @Autowired
//     private UserMapper userMapper;

//     // 测试插入数据，验证数据库连接
//     @Test
//     public void testInsert() {
//         User user = new User();
//         user.setUsername("admin");
//         user.setPassword("123456");
//         user.setUserRole("0"); // 系统管理员
//         user.setStatus("active"); // 激活
//         user.setCreateTime(LocalDateTime.now());
//         user.setDepartment("计算机学院");

//         int result = userMapper.insert(user);
//         System.out.println("插入成功，受影响行数：" + result);
//         System.out.println("插入的用户ID：" + user.getUserId()); // 自增主键自动返回
//     }

//     // 测试查询数据
//     @Test
//     public void testSelect() {
//         User user = userMapper.selectById(1L);
//         System.out.println("查询到的用户信息：" + user);
//     }
// }