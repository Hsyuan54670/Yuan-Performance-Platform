import { request, type ApiResponse } from "../utils/request";

export interface UserRow {
  id: number;
  username: string;
  nickname: string;
  role: string;
  status: string;
}

export interface UserCreatePayload {
  username: string;
  password: string;
  nickname: string;
  roleId: number;
}

export interface RoleRow {
  id: number;
  code: string;
  name: string;
  description: string;
}

export interface PermissionRow {
  id: number;
  code: string;
  name: string;
  module: string;
  description: string;
}

export interface RolePayload {
  code: string;
  name: string;
  description?: string;
}

export interface RolePermissionBindingPayload {
  permissionIds: number[];
}

export interface RoleMenuBindingPayload {
  menuIds: number[];
}

export interface UserStatusPayload {
  status: "ACTIVE" | "DISABLED";
}

export interface MenuNode {
  id: number;
  name: string;
  path: string;
  children?: MenuNode[];
}

export const listUsersApi = async (): Promise<UserRow[]> => {
  const { data } = await request.get<ApiResponse<UserRow[]>>("/auth/users");
  return data.data ?? [];
};

export const createUserApi = async (payload: UserCreatePayload): Promise<number | null> => {
  const { data } = await request.post<ApiResponse<number>>("/auth/users", payload);
  return data.data ?? null;
};

export const updateUserStatusApi = async (userId: number, payload: UserStatusPayload): Promise<void> => {
  await request.put<ApiResponse<null>>(`/auth/users/${userId}/status`, payload);
};

export const listRolesApi = async (): Promise<RoleRow[]> => {
  const { data } = await request.get<ApiResponse<RoleRow[]>>("/auth/roles");
  return data.data ?? [];
};

export const createRoleApi = async (payload: RolePayload): Promise<number | null> => {
  const { data } = await request.post<ApiResponse<number>>("/auth/roles", payload);
  return data.data ?? null;
};

export const updateRoleApi = async (roleId: number, payload: RolePayload): Promise<void> => {
  await request.put<ApiResponse<null>>(`/auth/roles/${roleId}`, payload);
};

export const deleteRoleApi = async (roleId: number): Promise<void> => {
  await request.delete<ApiResponse<null>>(`/auth/roles/${roleId}`);
};

export const getPermissionsApi = async (): Promise<PermissionRow[]> => {
  const { data } = await request.get<ApiResponse<PermissionRow[]>>("/auth/permissions");
  return data.data ?? [];
};

export const getRolePermissionIdsApi = async (roleId: number): Promise<number[]> => {
  const { data } = await request.get<ApiResponse<number[]>>(`/auth/roles/${roleId}/permissions`);
  return data.data ?? [];
};

export const updateRolePermissionIdsApi = async (roleId: number, payload: RolePermissionBindingPayload): Promise<void> => {
  await request.put<ApiResponse<null>>(`/auth/roles/${roleId}/permissions`, payload);
};

export const getMenuTreeApi = async (): Promise<MenuNode[]> => {
  const { data } = await request.get<ApiResponse<MenuNode[]>>("/auth/menus");
  return data.data ?? [];
};

export const getRoleMenuIdsApi = async (roleId: number): Promise<number[]> => {
  const { data } = await request.get<ApiResponse<number[]>>(`/auth/roles/${roleId}/menus`);
  return data.data ?? [];
};

export const updateRoleMenuIdsApi = async (roleId: number, payload: RoleMenuBindingPayload): Promise<void> => {
  await request.put<ApiResponse<null>>(`/auth/roles/${roleId}/menus`, payload);
};
