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

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.beansbinding.Property;

import com.moss.beans.messages.impl.Bucket;
import com.moss.beans.messages.impl.Buckets;
import com.moss.beans.messages.impl.MessageReport;

public class ValidationMessageBroker {
	private Log log = LogFactory.getLog(ValidationMessageBroker.class);
	
	public static class MessagesBucketName implements Comparable<MessagesBucketName>{
		private String name;

		public MessagesBucketName(String name) {
			super();
			this.name = name;
		}
		
		public int compareTo(MessagesBucketName o) {
			return o.name.compareTo(name);
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	private Map<Object, Map<Property<?, ?>, ValidationMessageReporter>> registry = new IdentityHashMap<Object, Map<Property<?,?>,ValidationMessageReporter>>();
	public static final MessagesBucketName DEFAULT_BUCKET = new MessagesBucketName("DEFAULT");
	
	private Buckets buckets = new Buckets();
	private List<ValidationMessageMonitor> monitors = new LinkedList<ValidationMessageMonitor>();
	
	
	public void add(ValidationMessageMonitor monitor){
		monitors.add(monitor);
	}
	
	public void remove(ValidationMessageMonitor monitor){
		monitors.remove(monitor);
	}
	public int numMessages(){
		return numMessages(DEFAULT_BUCKET);
	}
	public int numMessages(MessagesBucketName bucketName){
		Bucket bucket = buckets.bucket(bucketName);
		return bucket.getMessages().size();
	}
	
	public List<ValidationMessage> getReportsFor(ValidationMessageReporter reporter){
		List<ValidationMessage> messages = new LinkedList<ValidationMessage>();
		
		for(Bucket bucket: buckets.list()){
			for(MessageReport report: bucket.getMessages()){
				if(report.reporter==reporter){
					messages.add(report.message);
				}
			}
		}
		
		return messages;
	}
	
	public <T, V> void registerReporter(T bean, Property<T, V> property, ValidationMessageReporter reporter){
		if(bean==null) generateErrorMessage(bean, property, null, "You can't register a reporter on a null bean.");
		
		Map<Property<?, ?>, ValidationMessageReporter> propertyMap = registry.get(bean);
		if(propertyMap==null){
			propertyMap = new TreeMap<Property<?,?>, ValidationMessageReporter>(new PropertyComparator());
			registry.put(bean, propertyMap);
		}
		if(propertyMap.get(property)!=null){
			ValidationMessageReporter preExistingReporter = propertyMap.get(property); 
			if(preExistingReporter == reporter){
				log.warn("You have tried to register the same reporter for the same context more than once.  This could be indicative of some buggy code.  You should take a look at it.");
			}else{
				generateErrorMessage(bean, property, null, "A reporter has already been registered for this property");
			}
		}

		propertyMap.put(property, reporter);
	}
	
	public <T, V>  void unreport(T bean, Property<T, V> property, ValidationMessage message){
		for(Bucket bucket:buckets.list()){
			List<MessageReport> messagesToRemove = new LinkedList<MessageReport>();
			for(MessageReport report: bucket.getMessages()){
				if(report.message == message){
					report.reporter.clearMessage(message);
					messagesToRemove.add(report);
				}
			}
			bucket.getMessages().removeAll(messagesToRemove);
			for(MessageReport report:messagesToRemove){
				for(ValidationMessageMonitor monitor: monitors){
					monitor.clearMessage(report.reporter, report.message);
				}
			}
		}
	}
	
	
	
	public <T, V>  void report(T bean, Property<T, V> property, ValidationMessage message){
		report(bean, property, message, DEFAULT_BUCKET);
	}

	public <T, V>  void report(T bean, Property<T, V> property, ValidationMessage message, MessagesBucketName bucketName){
		Bucket bucket = buckets.bucket(bucketName);
		
		Map<Property<?, ?>, ValidationMessageReporter> propertyMap = registry.get(bean);
		
		if(propertyMap==null) generateErrorMessage(bean, property, message, "There is no message reporter for the message.  In fact, no reporters have been registered for any of this bean's properties.  The list of beans that have been entries is: " + registry.keySet());
		
		ValidationMessageReporter reporter = propertyMap.get(property);
		
		if(reporter==null) generateErrorMessage(bean, property, message, "There is no message reporter for the message.");
		
		reporter.display(message);
		bucket.getMessages().add(new MessageReport(message, reporter));
		
		for(ValidationMessageMonitor monitor:monitors){
			monitor.displayed(reporter, message);
		}
	}

	private <T, V> void generateErrorMessage(T bean, Property<T, V> property, ValidationMessage message, String error){
		String beanClass = "NA";
		String beanString = "NA";
		if(bean!=null){
			bean.getClass().getName();
			try{
				beanString = bean.toString();
			}catch(Exception e){
				beanString="There was an error calling toString(): " + e.getMessage();
			}
			
		}
		
		String propertyString = "";
		String messageString = "";
		if(message!=null)
			messageString = message.getMessage();
		try{
			propertyString = property.toString();
		}catch(Exception e){
			propertyString="There was an error calling toString(): " + e.getMessage();
		}
		
		throw new Error(error + "  Context: \n" + 
				"         Bean Class: " + beanClass + "\n" + 
				"        Bean.string: " + beanString + "\n" + 
				"    Property.string: " + propertyString + "\n" + 
				"            Message: " + messageString + "\n"
				);
	}

	public void clear(){
		clear(DEFAULT_BUCKET);
	}
	public void clear(MessagesBucketName bucketName){
		Bucket bucket = buckets.bucket(bucketName);
		
		for(MessageReport report: bucket.getMessages()){
			report.reporter.clearMessage(report.message);
		}
		bucket.getMessages().clear();
		
//		bucket.getMessages().clear();
//		for (Map<Property<?, ?>, ValidationMessageReporter> properties : registry.values()) {
//			for (ValidationMessageReporter reporter : properties.values()) {
//				reporter.clearMessages();
//			}
//		}
	}
	
	public String getMessage(int num) {
		return getMessage(num, DEFAULT_BUCKET);
	}
	public String getMessage(int num, MessagesBucketName bucketName) {
	
		Bucket bucket = buckets.bucket(bucketName);
		
		return bucket.getMessages().get(num).message.getMessage();
	}
	
	public ValidationMessageReporter getReporterForMessage(int num){
		return getReporterForMessage(num, DEFAULT_BUCKET);
	}

	public ValidationMessageReporter getReporterForMessage(int num, MessagesBucketName bucketName){
		Bucket bucket = buckets.bucket(bucketName);
		return bucket.getMessages().get(num).reporter;
	}
}

class PropertyComparator implements Comparator<Property<?,?>> {
	public int compare(Property<?, ?> o1, Property<?, ?> o2) {
		if(o1==null && o2 == null) return 0;
		else if(o1==null) return 1;
		else if(o2==null) return -1;
		if(o1.equals(o2)) return 0;
		return o1.toString().compareTo(o2.toString());
	}
}