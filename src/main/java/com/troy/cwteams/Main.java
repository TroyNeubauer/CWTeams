package com.troy.cwteams;


import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.Attribute;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.util.List;

class Main
{
	public static void error(String message)
	{
		System.out.println(Ansi.colorize("CWTeams: " + message, Attribute.BLACK_BACK(), Attribute.RED_BACK()));
	}

	public static void info(String message)
	{
		System.out.println("CWTeams: " + message);
	}

	public static void warn(String message)
	{
		System.out.println(Ansi.colorize("CWTeams: " + message, Attribute.TEXT_COLOR(255,140,0)));
	}


	public static void main(String[] args)
	{
		ArgumentParser parser = ArgumentParsers.newFor("CWTeams").build()
				.description("An automated team balancing program made for perverted cake wars by Troy Neubauer");

		parser.addArgument("--file")
				.dest("file").setDefault("cw.xlsx")
				.help("Specifies the input file to get the player list and ranking matrix");

		parser.addArgument("--max-deviation")
				.dest("maxDev").setDefault(1.0).type(Double.class)
				.help("Indicates the max deviation in the total ranking across teams");

		parser.addArgument("--limit")
				.dest("limitCount").setDefault(1000).type(Integer.class)
				.help("Sets a limit for the max number of permeated teams to be generated");

		try
		{
			Namespace res = parser.parseArgs(args);
			String fileStr = res.get("file");
			File cwFile = new File(fileStr);
			if (!cwFile.exists())
			{
				error("Failed to find file \"" + fileStr + "\"");
				System.exit(1);
			}
			info("Found input file \"" + fileStr + "\"");

			List<CWPlayer> players = RatingsReader.parsePlayers(cwFile);

			double maxDev = res.get("maxDev");
			int limitCount = res.get("limitCount");
			info("Using a max deviation of " + maxDev);
			info("Limiting output to " + limitCount + " permutations");

		}
		catch (ArgumentParserException e)
		{
			parser.handleError(e);
		}

	}

}
