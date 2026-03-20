package com.yuan.auth.service;

import com.yuan.auth.vo.SystemPermissionVO;
import com.yuan.common.result.R;

import java.util.List;

public interface PermissionService {

    R<List<SystemPermissionVO>> listPermissions();
}
