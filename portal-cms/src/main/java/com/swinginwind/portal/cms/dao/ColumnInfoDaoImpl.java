package com.swinginwind.portal.cms.dao;

import com.swinginwind.portal.cms.dto.ColumnInfoQueryDTO;
import com.swinginwind.portal.cms.entity.ColumnInfo;
import com.swinginwind.portal.common.dao.CustomBaseSqlDaoImpl;
import com.swinginwind.portal.common.entity.PageModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
/**
 * @author xujianfang
 * @desc ColumnInfoDaoImpl类 
 * @date 2017-03-16
 */

public class ColumnInfoDaoImpl extends CustomBaseSqlDaoImpl implements ColumnInfoDaoCustom  {

    public PageModel<ColumnInfo> queryColumnInfoPage(ColumnInfoQueryDTO columnInfoQueryDTO){
         Map<String,Object> map = new HashMap<String,Object>();
         StringBuilder hql = new StringBuilder();
         hql.append("select t from ColumnInfo t where 1=1 ");
         if(columnInfoQueryDTO != null){
        	 if(StringUtils.isNotBlank(columnInfoQueryDTO.getCode())){
        		 hql.append(" and t.code like :code ");
        		 map.put("code", "%"+columnInfoQueryDTO.getCode()+"%");
        	 }
        	 
        	 if(StringUtils.isNotBlank(columnInfoQueryDTO.getName())){
        		 hql.append(" and t.name like :name ");
        		 map.put("name", "%"+columnInfoQueryDTO.getName()+"%");
        	 }
        	 
        	 if(columnInfoQueryDTO.getLevel() != null){
        		 hql.append(" and t.level = :level ");
        		 map.put("level", columnInfoQueryDTO.getLevel());
        	 }
         }
         hql.append(" order by t.orderNo asc ");
         return this.queryForPageWithParams(hql.toString(),map,columnInfoQueryDTO.getCurrentPage(),columnInfoQueryDTO.getPageSize());
    }

    public List<ColumnInfo> queryColumnInfoList(ColumnInfoQueryDTO columnInfoQueryDTO){
         Map<String,Object> map = new HashMap<String,Object>();
         StringBuilder hql = new StringBuilder();
         hql.append("select t from ColumnInfo t where 1=1 and t.deleteFlag = 0 ");
         if(columnInfoQueryDTO != null){
        	 if(columnInfoQueryDTO.getIsRootColumnLike() != null && !columnInfoQueryDTO.getIsRootColumnLike()){
        		 if(StringUtils.isNotBlank(columnInfoQueryDTO.getRootColumnId())){
            		 hql.append(" and t.parent.id = :rootColumnId ");
            		 map.put("rootColumnId", columnInfoQueryDTO.getRootColumnId());
            	 }
        	 }else{
        		 if(StringUtils.isNotBlank(columnInfoQueryDTO.getRootColumnId())){
            		 hql.append(" and t.path like :rootColumnId ");
            		 map.put("rootColumnId", "%"+columnInfoQueryDTO.getRootColumnId()+"%");
            	 }
        	 }
        	 
        	 if(StringUtils.isNotBlank(columnInfoQueryDTO.getCode())){
        		 if(columnInfoQueryDTO.getIsCodeLike() != null && columnInfoQueryDTO.getIsCodeLike().equals(false)){
        			 hql.append(" and t.code = :code ");
            		 map.put("code", columnInfoQueryDTO.getCode());
        		 }else{
        			 hql.append(" and t.code like :code ");
            		 map.put("code", "%"+columnInfoQueryDTO.getCode()+"%");
        		 }
        	 }
        	 
        	 if(StringUtils.isNotBlank(columnInfoQueryDTO.getName())){
        		 hql.append(" and t.name like :name ");
        		 map.put("name", "%"+columnInfoQueryDTO.getName()+"%");
        	 }
        	 
        	 if(columnInfoQueryDTO.getLevel() != null){
        		 hql.append(" and t.level = :level ");
        		 map.put("level", columnInfoQueryDTO.getLevel());
        	 }
        	 
        	 if(columnInfoQueryDTO.getColumnId() != null){
        		 hql.append(" and t.id = :columnId ");
        		 map.put("columnId", columnInfoQueryDTO.getColumnId());
        	 }
         }
         hql.append(" order by t.orderNo asc ");
         return this.queryByMapParams(hql.toString(),map);
    }


}