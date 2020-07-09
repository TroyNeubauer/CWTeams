package com.troy.cwteams;


import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.Attribute;

class Main
{
	public static void main(String[] args)
	{
		System.out.println(Ansi.colorize("Hello World!", Attribute.YELLOW_TEXT()));
	}

}
