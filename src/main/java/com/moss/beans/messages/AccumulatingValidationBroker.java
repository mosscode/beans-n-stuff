/**
 * Copyright (C) 2013, Moss Computing Inc.
 *
 * This file is part of beans-n-stuff.
 *
 * beans-n-stuff is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * beans-n-stuff is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with beans-n-stuff; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package com.moss.beans.messages;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Property;


public class AccumulatingValidationBroker extends ValidationMessageBroker {
	
	public static void main(String[] args) {
		
	}
	private Map<Object, Map<Property, List<ValidationMessage>>> messageMap = new IdentityHashMap<Object, Map<Property,List<ValidationMessage>>>();
	
	public <T, V>  void report(T bean, Property<T, V> property, ValidationMessage message){
		Map<Property, List<ValidationMessage>> propertyMap = messageMap.get(bean);
		if(propertyMap==null){
			propertyMap = new IdentityHashMap<Property, List<ValidationMessage>>();
			messageMap.put(bean, propertyMap);
		}
		
		List<ValidationMessage> messagesForProperty = propertyMap.get(property);
		if(messagesForProperty==null){
			messagesForProperty = new LinkedList<ValidationMessage>();
			propertyMap.put(property, messagesForProperty);
		}
		
		messagesForProperty.add(message);
	}
	
	public StringBuffer combineMessages(Object object){
		
		if(messageMap.size()>0){
			StringBuffer text = new StringBuffer();
			for(Object bean: messageMap.keySet()){
				text.append(pathToObject(null, object, bean, 5));
				text.append("\n");
				Map<Property, List<ValidationMessage>> propertyMap = messageMap.get(bean);
				for(Property property:propertyMap.keySet()){
//					text.append(" ");
//					text.append(((BeanProperty)property).);
//					text.append("\n");
					List<ValidationMessage> messagesForProperty = propertyMap.get(property);
					for(ValidationMessage nextMessage: messagesForProperty){
						text.append("    ");
						text.append(nextMessage.getMessage());
						text.append("\n");
					}
				}
			}
			return text;
		}else{
			return null;
		}
		
	}
	
	private String pathToObject(String base, Object root, Object value, int maxDistance) {
		if(maxDistance==0) return null;
		try {
			BeanInfo rootInfo = Introspector.getBeanInfo(root.getClass());
			PropertyDescriptor[] properties = rootInfo.getPropertyDescriptors();
			for (PropertyDescriptor property : properties) {
				Object propertyValue = property.getReadMethod().invoke(root, new Object[]{});
				String path;
				if(base==null){
					path=property.getDisplayName();
				}
				else {
					path =  base + "." + property.getDisplayName();
				}
				if(propertyValue==value){
					return path;
				}else if(propertyValue!=null){
					String result = pathToObject(path, propertyValue, value, maxDistance-1);
					if(result!=null) return result;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
