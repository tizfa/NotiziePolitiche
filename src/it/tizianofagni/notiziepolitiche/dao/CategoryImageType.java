/*
 * Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package it.tizianofagni.notiziepolitiche.dao;

import it.tizianofagni.notiziepolitiche.R;




public enum CategoryImageType {
	API(1),
	FLI(2),
	IDV(3),
	LEGA(4),
	MPA(5),
	PD(6),
	PDL(7),
	SEL(8),
	UDC(9);
	
	int val;
	
	private CategoryImageType(int val) {
		this.val = val;
	}
	
	public int getIntValue() {
		return val;
	}
	
	public int getResourceIDValue() {
		if (val == 1)
			return R.drawable.api;
		else if (val == 2)
			return R.drawable.futuroeliberta;
		else if (val == 3)
			return R.drawable.idv;
		else if (val == 4)
			return R.drawable.lega;
		else if (val == 5)
			return R.drawable.mpa;
		else if (val == 6)
			return R.drawable.pd;
		else if (val == 7)
			return R.drawable.pdl;
		else if (val == 8)
			return R.drawable.sinistra_ecologia_e_liberta;
		else if (val == 9)
			return R.drawable.udc;
		
		// Should never reach this point.
		return -1;
		
	}
	
	public static CategoryImageType fromIntValue(int value) {
		if (value < 1 || value > values().length)
			throw new RuntimeException("The specified value is invalid: "+value);
		
		
		CategoryImageType[] types = values();
		for (int i = 0; i < types.length; i++) {
			if (types[i].getIntValue() == value)
				return types[i];
		}
		
		// Should never reach this point.
		return null;
	}
}
