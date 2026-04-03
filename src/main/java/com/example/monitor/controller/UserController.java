package com.example.monitor.controller;

// import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.monitor.entity.User;
import com.example.monitor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户接口控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 新增用户
     */
    @PostMapping
    public String addUser(@RequestBody User user) {
        user.setCreateTime(LocalDateTime.now());
        boolean save = userService.save(user);
        return save ? "新增成功" : "新增失败";
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getById(id);
    }

    /**
     * 查询所有用户
     */
    @GetMapping
    public List<User> listAllUsers() {
        return userService.list();
    }

    /**
     * 根据用户名查询用户
     */
    @GetMapping("/username/{username}")
    public User getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username);
    }

    /**
     * 更新用户
     */
    @PutMapping
    public String updateUser(@RequestBody User user) {
        boolean update = userService.updateById(user);
        return update ? "更新成功" : "更新失败";
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        boolean remove = userService.removeById(id);
        return remove ? "删除成功" : "删除失败";
    }
}