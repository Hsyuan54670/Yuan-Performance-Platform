package com.yuan.auth.service;

import com.yuan.auth.vo.SystemMenuVO;
import com.yuan.common.result.R;

import java.util.List;

public interface MenuService {

    R<List<SystemMenuVO>> listMenus();

    List<SystemMenuVO> listMenusByRoleIds(List<Long> roleIds);
}
