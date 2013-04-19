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

package it.tizianofagni.notiziepolitiche;

import it.tizianofagni.notiziepolitiche.exception.InvalidParameterException;

/**
 * This class represents a software version.
 *
 * @author Tiziano Fagni
 */
public class SoftwareVersion implements Comparable<SoftwareVersion>
{

	/**
	 * The major version of the software.
	 */
	private  int major = 1;

	/**
	 * The minor version of the software.
	 */
	private  int minor = 0;


	/**
	 * The subminor version of the software.
	 */
	private  int subminor = 0;


	/**
	 * Build a new object representing a particular version of the software.<br>
	 * The version representation has the following form: "major"."minor"."subminor"
	 *
	 * @param major The major value.
	 * @param minor The minor value.
	 * @param subminor The subminor value.
	 */
	public SoftwareVersion(int major, int minor, int subminor)
	{
		this.major = major;
		this.minor = minor;
		this.subminor = subminor;
	}

	
	public SoftwareVersion(String version) throws InvalidParameterException {
		if (version == null)
			throw new InvalidParameterException("The specified version is 'null'");
		
		String[] numbers = version.split("\\.");
		if (numbers.length != 3)
			throw new InvalidParameterException("The specified version format is not supported: "+version);
		
		try {
			major = Integer.parseInt(numbers[0]);
			minor = Integer.parseInt(numbers[1]);
			subminor = Integer.parseInt(numbers[2]);
		} catch (Exception e) {
			throw new InvalidParameterException("The specified version format is not supported: "+version);
		}
	}
	

	/**
	 * Get the major number of the software.
	 *
	 * @return The major number.
	 */
	public int getMajor()
	{
		return major;
	}


	/**
	 * Get the minor number of the software.
	 *
	 * @return The minor number.
	 */
	public int getMinor()
	{
		return minor;
	}


	/**
	 * Get the subminor number of the software.
	 *
	 * @return The subminor number.
	 */
	public int getSubminor()
	{
		return subminor;
	}



	@Override
	public String toString()
	{
		String v = ""+major+"."+minor+"."+subminor;
		return v;
	}



	public int compareTo(SoftwareVersion o)
	{
		if (major < o.major)
			return -1;
		else if (major == o.major)
		{
			if (minor < o.minor)
				return -1;
			else if (minor == o.minor)
			{
				if (subminor < o.subminor)
					return -1;
				else if (subminor == o.subminor)
					return 0;
				else
					return 1;
			}
			else
				return 1;
		}
		else
			return 1;
	}


	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof SoftwareVersion))
			return false;

		SoftwareVersion o = (SoftwareVersion) obj;
		return (compareTo(o) == 0);
	}


	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}



}
