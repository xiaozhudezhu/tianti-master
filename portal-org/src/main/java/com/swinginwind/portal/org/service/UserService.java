package com.swinginwind.portal.org.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.swinginwind.portal.common.dao.CustomBaseSqlDaoImpl;
import com.swinginwind.portal.common.entity.PageModel;
import com.swinginwind.portal.common.service.CommonService;
import com.swinginwind.portal.org.dao.UserDao;
import com.swinginwind.portal.org.dto.UserQueryDTO;
import com.swinginwind.portal.org.entity.Resource;
import com.swinginwind.portal.org.entity.Role;
import com.swinginwind.portal.org.entity.User;


@Service
public class UserService extends CommonService<User,String>{

	@Autowired
	private UserDao userDao;
	
	@Autowired
	public void setUserDao(UserDao userDao) {
		super.setCommonDao(userDao);
	}
	
	public User findUserByName(String userName){
		User user = null;
		List<User> userList = this.userDao.findUserByName(userName);
		if(userList != null && userList.size() > 0){
			user = userList.get(0);
		}
		return user;
	}

	public List<User> findUsers(Map<String, Object> params) {
		return userDao.findUsers(params);
	}
	
	public List<User> updateStatus(String[] ids, Integer status){
		List<User> users = new ArrayList<User>();
		if(ids != null){
			for(String id : ids){
				User user = this.find(id);
				user.setStatus(status);
				this.update(user);
				users.add(user);
			}
		}
		return users;
	}
	
	public List<Resource> findResourcesByUserId(String userId) {
		return userDao.findResourcesByUserId(userId);
	}
	
	public List<Role> findRolesByUserId(String userId) {
		return userDao.findRolesByUserId(userId);
	}
	
	public Map<String, Object> findResourceMap(String userId){
		Map<String, Object> map = new HashMap<String, Object>();
		
		List<Resource> resources = findResourcesByUserId(userId);
		if(resources != null && !resources.isEmpty()){
			for(Resource r : resources){
				map.put(r.getId(), r.getName());
			}
		}
		
		return map;
	}
	
	/**
	 * 根据用户信息查询分页信息
	 * @param userQueryDTO
	 * @return
	 */
	public PageModel<User> queryUserPage(UserQueryDTO userQueryDTO){
		return this.userDao.queryUserPage(userQueryDTO);
	}
	
}
