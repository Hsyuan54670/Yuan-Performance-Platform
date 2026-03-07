import { menuTree, roles, users } from "../mock/data";
import { withMockDelay } from "../utils/request";

export interface UserRow {
  id: number;
  username: string;
  nickname: string;
  role: string;
  status: string;
}

export interface RoleRow {
  id: number;
  name: string;
  description: string;
}

export interface MenuNode {
  id: number;
  name: string;
  path: string;
}

export const listUsersApi = async (): Promise<UserRow[]> => withMockDelay(users, 170);

export const listRolesApi = async (): Promise<RoleRow[]> => withMockDelay(roles, 170);

export const getMenuTreeApi = async (): Promise<MenuNode[]> => withMockDelay(menuTree, 170);
