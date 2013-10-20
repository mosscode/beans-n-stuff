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

import java.awt.FlowLayout;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTextField;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.beansbinding.PropertyStateListener;

public class ArrayElementProperty<S, V> extends Property<S, V>{
	static class Numbers{
		Integer[] values={44, 55, 66, 77};

		public final Integer[] getValues() {
			return values;
		}

		public final void setValues(Integer[] values) {
			this.values = values;
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		
		
		JFrame window = new JFrame();
		JTextField field = new JTextField("324324324");
		window.getContentPane().setLayout(new FlowLayout());
		window.getContentPane().add(field);
		window.setSize(300, 300);
		window.setVisible(true);
		
		
		
		ArrayElementProperty<Numbers, Integer> element3 = new ArrayElementProperty<Numbers, Integer>(Numbers.class, Integer.class, "values", 3);
		
		final Numbers nums = new Numbers();
		
		System.out.println(element3.getValue(nums));
		element3.setValue(nums, 88);
		System.out.println(element3.getValue(nums));
		System.out.println(nums.values[3]);

		Binding<Numbers, Integer, JTextField, String> binding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, 
				nums, element3,
				field, BeanProperty.<JTextField, String>create("text"));
		binding.bind();
		
		new Thread(){
			@Override
			public void run() {
				while(true){
					try {
						Thread.sleep(1000);
						System.out.println(nums.values[3]);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
//		binding.setConverter(new Converter<Integer, String>(){
//			
//		});
	}
	
	Class<? extends S> ownerClass;
	Class<? extends V> valueClass;
	String propertyName;
	int elementNum;
	PropertyDescriptor prop;
	
	@Override
	public boolean equals(Object obj) {
		if(obj==null) return false;
		if(!(obj instanceof ArrayElementProperty)) return false;

		ArrayElementProperty other = (ArrayElementProperty) obj;
		if(!other.propertyName.equals(propertyName)) return false;
		if(!other.valueClass.equals(valueClass)) return false;
		if(!other.ownerClass.equals(ownerClass)) return false;
		if(other.elementNum!=elementNum) return false;
		return true;
	}
	@Override
	public String toString() {
		return ownerClass.getName() + ".properties." + valueClass.getName() + ":" + propertyName + "[" + elementNum + "]";
	}
	static class ListenerBinding<S> {
		WeakReference<PropertyStateListener> listener;
		WeakReference<S> owner;
	}
	
	private List<ListenerBinding> listeners = new LinkedList<ListenerBinding>();
	
	public ArrayElementProperty(Class<? extends S> ownerClass, Class<? extends V> valueClass, String propertyName, int elementNum) throws IntrospectionException {
		super();
		this.ownerClass = ownerClass;
		this.valueClass = valueClass;
		this.propertyName = propertyName;
		this.elementNum = elementNum;
		BeanInfo bi = Introspector.getBeanInfo(ownerClass);
		PropertyDescriptor[] properties = bi.getPropertyDescriptors();
		for (PropertyDescriptor prop : properties) {
			if(prop.getName().equals(propertyName)){
				this.prop = prop;
			}
		}
		if(prop==null)
			throw new RuntimeException("Could not find property named " + propertyName);
	}

	@Override
	public void addPropertyStateListener(S arg0, PropertyStateListener arg1) {
		ListenerBinding<S> binding = new ListenerBinding<S>();
		binding.owner = new WeakReference<S>(arg0);
		binding.listener = new WeakReference<PropertyStateListener>(arg1);
		synchronized(listeners){
			listeners.add(binding);
		}
	}

	@Override
	public PropertyStateListener[] getPropertyStateListeners(S arg0) {
		List<PropertyStateListener> ownerListeners = new LinkedList<PropertyStateListener>();
		synchronized(listeners){
			for(ListenerBinding<S> binding: listeners){
				if(binding.owner.get() == arg0)
					ownerListeners.add(binding.listener.get());
			}
		}
		return ownerListeners.toArray(new PropertyStateListener[]{});
	}
	
	@Override
	public void removePropertyStateListener(S arg0, PropertyStateListener arg1) {
		synchronized(listeners){
			List<ListenerBinding> toRemove = new LinkedList<ListenerBinding>();
			for(ListenerBinding<S> binding: listeners){
				if(binding.owner.get() == arg0 && binding.listener.get()==arg1)
					toRemove.add(binding);
			}
			listeners.removeAll(toRemove);
		}
	}

	private V[] getArray(S arg0){
		try{
			V[] array = (V[]) prop.getReadMethod().invoke(arg0, new Object[]{});
			return array;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public V getValue(S arg0) {
		return getArray(arg0)[elementNum];
	}

	@Override
	public Class<? extends V> getWriteType(S arg0) {
		return valueClass;
	}

	@Override
	public boolean isReadable(S arg0) {
		return true;
	}

	@Override
	public boolean isWriteable(S arg0) {
		return true;
	}


	@Override
	public void setValue(S arg0, V arg1) {
		getArray(arg0)[elementNum] = arg1;
	}

}
