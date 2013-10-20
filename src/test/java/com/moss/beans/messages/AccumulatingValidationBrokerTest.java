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

import org.jdesktop.beansbinding.BeanProperty;

import com.moss.beans.messages.AccumulatingValidationBroker;
import com.moss.beans.messages.ValidationMessage;

import junit.framework.TestCase;

public class AccumulatingValidationBrokerTest extends TestCase {
	public void testExecute(){
		AccumulatingValidationBroker b = new AccumulatingValidationBroker();
		class City{
		}
		class Address{
			private City city;
			public City getCity() {
				return city;
			}
			public void setCity(City city) {
				this.city = city;
			}
		}
		class Person{
			private Address address;

			public final Address getAddress() {
				return address;
			}

			public final void setAddress(Address address) {
				this.address = address;
			}
			
		}
		Person p = new Person();
		Address a = new Address();
		City c = new City();
		p.setAddress(a);
		a.setCity(c);
		b.report(c, BeanProperty.<City, String>create("address1"), new ValidationMessage("Address is bad"));
		System.out.println("person" + b.combineMessages(p));
	}
}
