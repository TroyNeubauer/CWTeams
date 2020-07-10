package com.troy.cwteams;

public class CWPlayer implements Comparable<CWPlayer>
{
	public final String realName;
	public final String username;

	public final double pvp;
	public final double gamesense;
	public final double teamwork;

	public CWPlayer(String realName, String username, double pvp, double gamesense, double teamwork)
	{
		this.realName = realName;
		this.username = username;
		this.pvp = pvp;
		this.gamesense = gamesense;
		this.teamwork = teamwork;
	}

	public double getOverall()
	{
		//Average
		return (pvp + gamesense + teamwork) / 3.0;
	}

	@Override
	public int compareTo(CWPlayer cwPlayer)
	{
		return Double.compare(this.getOverall(), cwPlayer.getOverall());
	}
}
